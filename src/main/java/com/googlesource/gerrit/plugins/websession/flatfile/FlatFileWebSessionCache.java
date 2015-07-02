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
import com.google.gerrit.common.Nullable;
import com.google.gerrit.httpd.WebSessionManager;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

@Singleton
public class FlatFileWebSessionCache implements
    Cache<String, WebSessionManager.Val> {
  private static final Logger log = LoggerFactory
      .getLogger(FlatFileWebSessionCache.class);

  private final Path dir;
  private final long maxAge;

  @Inject
  public FlatFileWebSessionCache(@WebSessionDir Path dir,
      @WebSessionMaxAge long maxAge) {
    this.dir = dir;
    this.maxAge = maxAge;
    if (Files.notExists(dir)) {
      log.info(dir + " not found. Creating it.");
      try {
        Files.createDirectory(dir);
      } catch (IOException e) {
        log.error("Impossible to create directory " + dir, e);
      }
    }
  }

  @Override
  public ConcurrentMap<String, Val> asMap() {
    ConcurrentMap<String, Val> map = new ConcurrentHashMap<>();
    for (Path path : listFiles()) {
      Val v = readFile(path);
      if (v != null) {
        map.put(path.getFileName().toString(), v);
      }
    }
    return map;
  }

  @Override
  public void cleanUp() {
    DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
      DateTime threshold = new DateTime().minus(maxAge);

      @Override
      public boolean accept(Path path) throws IOException {
        BasicFileAttributes pathAttributes =
            Files.readAttributes(path, BasicFileAttributes.class);
        DateTime lastAccessTime =
            new DateTime(pathAttributes.lastAccessTime().toMillis());
        return lastAccessTime.isBefore(threshold);
      }
    };
    List<Path> toDelete = listFiles(filter);
    for (Path path : toDelete) {
      deleteFile(path);
    }
  }

  @Override
  public Val get(String key, Callable<? extends Val> valueLoader)
      throws ExecutionException {
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
      Path path = dir.resolve((String) key);
      return readFile(path);
    }
    return null;
  }

  @Override
  public void invalidate(Object key) {
    if (key instanceof String) {
      deleteFile(dir.resolve((String) key));
    }
  }

  @Override
  public void invalidateAll() {
    for (Path path : listFiles()) {
      deleteFile(path);
    }
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
      Path tempFile =
          Files.createTempFile(dir, UUID.randomUUID().toString(), null);
      try (OutputStream fileStream = Files.newOutputStream(tempFile);
          ObjectOutputStream objStream = new ObjectOutputStream(fileStream)) {
        objStream.writeObject(value);
        Files.move(tempFile, tempFile.resolveSibling(key),
            StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.warn("Cannot put into cache " + dir, e);
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
    return listFiles().size();
  }

  @Override
  public CacheStats stats() {
    log.warn("stats() unimplemented");
    return null;
  }

  private Val readFile(Path path) {
    if (Files.exists(path)) {
      try (InputStream fileStream = Files.newInputStream(path);
          ObjectInputStream objStream = new ObjectInputStream(fileStream)) {
        return (Val) objStream.readObject();
      } catch (ClassNotFoundException e) {
        log.warn("Entry " + path + " in cache " + dir + " has an incompatible "
            + "class and can't be deserialized. Invalidating entry.");
        invalidate(path.getFileName().toString());
      } catch (IOException e) {
        log.warn("Cannot read cache " + dir, e);
      }
    }
    return null;
  }

  private void deleteFile(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.error("Error trying to delete " + path + " from " + dir, e);
    }
  }

  List<Path> listFiles() {
    DirectoryStream.Filter<Path> all = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
        return true;
      }
    };
    return listFiles(all);
  }

  private List<Path> listFiles(Filter<Path> filter) {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> dirStream =
        Files.newDirectoryStream(dir, filter)) {
      for (Path path : dirStream) {
        files.add(path);
      }
    } catch (IOException e) {
      log.error("Cannot list files in cache " + dir, e);
    }
    return files;
  }
}
