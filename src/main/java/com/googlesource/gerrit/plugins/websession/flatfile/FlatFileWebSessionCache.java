// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.websession.flatfile;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.httpd.WebSessionManager;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Singleton
public class FlatFileWebSessionCache implements Cache<String, WebSessionManager.Val> {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  /** Provides static methods to set the system clock for testing purposes only. */
  static class TimeMachine {
    private static Clock clock = Clock.systemDefaultZone();

    private TimeMachine() {
      throw new IllegalAccessError("Utility class. Not meant to be instantiated.");
    }

    static Instant now() {
      return Instant.now(getClock());
    }

    static void useFixedClockAt(Instant instant) {
      clock = Clock.fixed(instant, ZoneId.systemDefault());
    }

    static void useSystemDefaultZoneClock() {
      clock = Clock.systemDefaultZone();
    }

    private static Clock getClock() {
      return clock;
    }
  }

  private final Path websessionsDir;

  @Inject
  public FlatFileWebSessionCache(@WebSessionDir Path websessionsDir) throws IOException {
    this.websessionsDir = websessionsDir;
    Files.createDirectories(websessionsDir);
  }

  @Override
  public ConcurrentMap<String, Val> asMap() {
    return sessionStream()
        .map(path -> new SimpleImmutableEntry<>(path.getFileName().toString(), readFile(path)))
        .filter(entry -> entry.getValue() != null)
        .collect(
            Collectors.toConcurrentMap(
                SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
  }

  @Override
  public void cleanUp() {
    foreachSession(
        path -> {
          Val val = readFile(path);
          if (val != null) {
            Instant expires = Instant.ofEpochMilli(val.getExpiresAt());
            if (expires.isBefore(TimeMachine.now())) {
              deleteFile(path);
            }
          }
        });
  }

  @Override
  public Val get(String key, Callable<? extends Val> valueLoader) throws ExecutionException {
    Val value = getIfPresent(key);
    if (value == null) {
      try {
        value = valueLoader.call();
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }
    return value;
  }

  @Override
  public ImmutableMap<String, Val> getAllPresent(Iterable<?> keys) {
    ImmutableMap.Builder<String, Val> mapBuilder = new ImmutableMap.Builder<>();
    for (Object key : keys) {
      Val v = getIfPresent(key);
      if (v != null) {
        mapBuilder.put((String) key, v);
      }
    }
    return mapBuilder.build();
  }

  @Override
  @Nullable
  public Val getIfPresent(Object key) {
    if (key instanceof String) {
      Path path = websessionsDir.resolve((String) key);
      return readFile(path);
    }
    return null;
  }

  @Override
  public void invalidate(Object key) {
    if (key instanceof String) {
      deleteFile(websessionsDir.resolve((String) key));
    }
  }

  @Override
  public void invalidateAll() {
    foreachSession(this::deleteFile);
  }

  @Override
  public void invalidateAll(Iterable<?> keys) {
    for (Object key : keys) {
      invalidate(key);
    }
  }

  @Override
  public void put(String key, Val value) {
    try {
      Path tempFile = Files.createTempFile(websessionsDir, UUID.randomUUID().toString(), null);
      try (OutputStream fileStream = Files.newOutputStream(tempFile);
          ObjectOutputStream objStream = new ObjectOutputStream(fileStream)) {
        objStream.writeObject(value);
        Files.move(
            tempFile,
            tempFile.resolveSibling(key),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      }
    } catch (IOException e) {
      log.atWarning().withCause(e).log("Cannot put into cache %s", websessionsDir);
    }
  }

  @Override
  public void putAll(Map<? extends String, ? extends Val> keys) {
    for (Entry<? extends String, ? extends Val> e : keys.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public long size() {
    return sessionStream().count();
  }

  @Override
  public CacheStats stats() {
    log.atWarning().log("stats() unimplemented");
    return null;
  }

  private Val readFile(Path path) {
    if (path.toFile().exists()) {
      try (InputStream fileStream = Files.newInputStream(path);
          ObjectInputStream objStream = new ObjectInputStream(fileStream)) {
        return (Val) objStream.readObject();
      } catch (ClassNotFoundException e) {
        log.atWarning().log(
            "Entry %s in cache %s has an incompatible class and can't be"
                + " deserialized. Invalidating entry.",
            path, websessionsDir);
        log.atFine().withCause(e).log("Exception message %s", e.getMessage());
        invalidate(path.getFileName().toString());
      } catch (IOException e) {
        log.atWarning().withCause(e).log("Cannot read cache %s", path);
      }
    }
    return null;
  }

  private void deleteFile(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.atSevere().withCause(e).log("Error trying to delete %s from %s", path, websessionsDir);
    }
  }

  private void foreachSession(Consumer<Path> sessionPath) {
    try (DirectoryStream<Path> dirStream = sessionDirectoryStream()) {
      dirStream.forEach(sessionPath);
    } catch (IOException e) {
      log.atSevere().withCause(e).log("Cannot list files in cache %s", websessionsDir);
    }
  }

  private Stream<Path> sessionStream() {
    try {
      return StreamSupport.stream(
          sessionDirectoryStream().spliterator(), false /* single-threaded */);
    } catch (IOException e) {
      log.atSevere().withCause(e).log("Cannot traverse files in cache %s", websessionsDir);
      return Stream.empty();
    }
  }

  private DirectoryStream<Path> sessionDirectoryStream() throws IOException {
    return Files.newDirectoryStream(websessionsDir);
  }
}
