// Copyright (C) 2015 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSessionCache.TimeMachine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FlatFileWebSessionCacheTest {

  private static final int DEFAULT_KEYS_SIZE = 10;

  private static final String EXISTING_KEY = "aSceprtBc02YaMY573T5jfW64ZudJfPbDq";
  private static final String EMPTY_KEY = "aOc2prqlZRpSO3LpauGO5efCLs1L9r9KkG";
  private static final String NEW_KEY = "abcde12345";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private FlatFileWebSessionCache flatFileWebSessionCache;
  private Path dir;

  @Before
  public void createFlatFileWebSessionCache() throws Exception {
    dir = tempFolder.newFolder("websessions").toPath();
    flatFileWebSessionCache = new FlatFileWebSessionCache(dir);
  }

  @Test
  public void asMapTest() throws Exception {
    loadKeyToCacheDir(EMPTY_KEY);
    assertThat(flatFileWebSessionCache.asMap()).isEmpty();

    loadKeyToCacheDir(EXISTING_KEY);
    assertThat(flatFileWebSessionCache.asMap()).containsKey(EXISTING_KEY);
  }

  @Test
  public void constructorCreateDir() throws IOException {
    Path testDir = Paths.get("tmp");
    flatFileWebSessionCache = new FlatFileWebSessionCache(testDir);
    assertThat(Files.exists(testDir)).isTrue();
    Files.deleteIfExists(testDir);
  }

  @Test
  public void cleanUpTest() throws Exception {
    loadKeyToCacheDir(EXISTING_KEY);
    try {
      long existingKeyExpireAt = flatFileWebSessionCache.getIfPresent(EXISTING_KEY).getExpiresAt();
      TimeMachine.useFixedClockAt(
          Instant.ofEpochMilli(existingKeyExpireAt).minus(1, ChronoUnit.HOURS));
      flatFileWebSessionCache.cleanUp();
      assertThat(isDirEmpty(dir)).isFalse();

      TimeMachine.useFixedClockAt(
          Instant.ofEpochMilli(existingKeyExpireAt).plus(1, ChronoUnit.HOURS));
      flatFileWebSessionCache.cleanUp();
      assertThat(isDirEmpty(dir)).isTrue();
    } finally {
      TimeMachine.useSystemDefaultZoneClock();
    }
  }

  @Test
  public void getAllPresentTest() throws Exception {
    loadKeyToCacheDir(EMPTY_KEY);
    loadKeyToCacheDir(EXISTING_KEY);
    List<String> keys = ImmutableList.of(EMPTY_KEY, EXISTING_KEY);
    assertThat(flatFileWebSessionCache.getAllPresent(keys).size()).isEqualTo(1);
    assertThat(flatFileWebSessionCache.getAllPresent(keys)).containsKey(EXISTING_KEY);
  }

  @Test
  public void getIfPresentObjectNonStringTest() throws Exception {
    assertThat(flatFileWebSessionCache.getIfPresent(new Object())).isNull();
  }

  @Test
  public void getIfPresentTest() throws Exception {
    loadKeyToCacheDir(EXISTING_KEY);
    assertThat(flatFileWebSessionCache.getIfPresent(EXISTING_KEY)).isNotNull();
  }

  @Test
  public void getTest() throws Exception {
    class ValueLoader implements Callable<Val> {
      @Override
      public Val call() throws Exception {
        return null;
      }
    }
    assertThat(flatFileWebSessionCache.get(EXISTING_KEY, new ValueLoader())).isNull();

    loadKeyToCacheDir(EXISTING_KEY);
    assertThat(flatFileWebSessionCache.get(EXISTING_KEY, new ValueLoader())).isNotNull();
  }

  @Test(expected = ExecutionException.class)
  public void getTestCallableThrowsException() throws Exception {
    class ValueLoader implements Callable<Val> {
      @Override
      public Val call() throws Exception {
        throw new Exception();
      }
    }
    assertThat(flatFileWebSessionCache.get(EXISTING_KEY, new ValueLoader())).isNull();
  }

  @Test
  public void invalidateAllCollectionTest() throws Exception {
    List<String> keys = createKeysCollection();
    flatFileWebSessionCache.invalidateAll(keys);
    assertThat(isDirEmpty(dir)).isTrue();
  }

  @Test
  public void invalidateAllTest() throws Exception {
    createKeysCollection();
    flatFileWebSessionCache.invalidateAll();
    assertThat(isDirEmpty(dir)).isTrue();
  }

  @Test
  public void invalidateTest() throws Exception {
    Path fileToDelete = Files.createFile(dir.resolve(EXISTING_KEY));
    assertThat(Files.exists(fileToDelete)).isTrue();
    flatFileWebSessionCache.invalidate(EXISTING_KEY);
    assertThat(Files.exists(fileToDelete)).isFalse();
  }

  @Test
  public void invalidateTestObjectNotString() throws Exception {
    createKeysCollection();
    assertThat(flatFileWebSessionCache.size()).isEqualTo(DEFAULT_KEYS_SIZE);
    flatFileWebSessionCache.invalidate(new Object());
    assertThat(flatFileWebSessionCache.size()).isEqualTo(DEFAULT_KEYS_SIZE);
  }

  @Test
  public void putTest() throws Exception {
    loadKeyToCacheDir(EXISTING_KEY);
    Val val = flatFileWebSessionCache.getIfPresent(EXISTING_KEY);
    flatFileWebSessionCache.put(NEW_KEY, val);
    assertThat(flatFileWebSessionCache.getIfPresent(NEW_KEY)).isNotNull();
  }

  @Test
  public void putAllTest() throws Exception {
    loadKeyToCacheDir(EXISTING_KEY);
    Val val = flatFileWebSessionCache.getIfPresent(EXISTING_KEY);
    Map<String, Val> sessions = ImmutableMap.of(NEW_KEY, val);
    flatFileWebSessionCache.putAll(sessions);
    assertThat(flatFileWebSessionCache.asMap()).containsKey(NEW_KEY);
  }

  @Test
  public void sizeTest() throws Exception {
    createKeysCollection();
    assertThat(flatFileWebSessionCache.size()).isEqualTo(DEFAULT_KEYS_SIZE);
  }

  @Test
  public void statTest() throws Exception {
    assertThat(flatFileWebSessionCache.stats()).isNull();
  }

  private List<String> createKeysCollection() throws IOException {
    List<String> keys = new ArrayList<>();
    for (int i = 0; i < DEFAULT_KEYS_SIZE; i++) {
      Path tmp = Files.createTempFile(dir, "cache", null);
      keys.add(tmp.getFileName().toString());
    }
    return keys;
  }

  private void emptyAndDelete(Path dir) throws IOException {
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private boolean isDirEmpty(final Path dir) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
      return !dirStream.iterator().hasNext();
    }
  }

  private Path loadKeyToCacheDir(String key) throws IOException {
    if (key.equals(EMPTY_KEY)) {
      return Files.createFile(dir.resolve(EMPTY_KEY));
    }
    try (InputStream in = loadFile(key)) {
      Path target = dir.resolve(key);
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      return target;
    }
  }

  private InputStream loadFile(String file) {
    return this.getClass().getResourceAsStream("/" + file);
  }
}
