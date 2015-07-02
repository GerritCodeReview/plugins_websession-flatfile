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

import static java.util.concurrent.TimeUnit.HOURS;

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.httpd.WebSessionManager.Val;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class FlatFileWebSessionCacheTest {

  private static final int DEFAULT_KEYS_SIZE = 10;
  private static final long MAX_AGE_MS = HOURS.toMillis(12);

  private FlatFileWebSessionCache flatFileWebSessionCache;
  private Path dir;
  private String key;
  private String existingKey;

  @Before
  public void createFlatFileWebSessionCache() throws Exception {
    dir = Files.createTempDirectory("websessions");
    key = "aOc2prqlZRpSO3LpauGO5efCLs1L9r9KkG";
    existingKey = "aSceprtBc02YaMY573T5jfW64ZudJfPbDq";
    flatFileWebSessionCache = new FlatFileWebSessionCache(dir, MAX_AGE_MS);
  }

  @Test
  public void asMapTest() throws Exception {
    Files.createFile(dir.resolve(key));
    assertThat(flatFileWebSessionCache.asMap()).isEmpty();

    loadExistingKeyToCacheDir();
    assertThat(flatFileWebSessionCache.asMap()).containsKey(existingKey);
  }

  @Test
  public void constructorCreateDir() throws IOException {
    Path testtDir = Paths.get("tmp");
    flatFileWebSessionCache = new FlatFileWebSessionCache(testtDir, MAX_AGE_MS);
    assertThat(Files.exists(testtDir)).isTrue();
    Files.deleteIfExists(testtDir);
  }

  @Test
  public void cleanUpTest() throws Exception {
    createOutdatedCacheFiles();
    flatFileWebSessionCache.cleanUp();
    assertThat(isDirEmpty(dir)).isTrue();
  }

  @Test
  public void getAllPresentTest() throws Exception {
    Files.createFile(dir.resolve(key));
    loadExistingKeyToCacheDir();
    List<String> keys = Arrays.asList(new String[] {key, existingKey});
    assertThat(flatFileWebSessionCache.getAllPresent(keys)).containsKey(
        existingKey);
  }

  @Test
  public void getIfPresentKeyDoesNotExistTest() throws Exception {
    assertThat(flatFileWebSessionCache.getIfPresent(key)).isNull();
  }

  @Test
  public void getIfPresentObjectNonStringTest() throws Exception {
    Path path = dir.resolve(key);
    assertThat(flatFileWebSessionCache.getIfPresent(path)).isNull();
  }

  @Test
  public void getIfPresentTest() throws Exception {
    loadExistingKeyToCacheDir();
    assertThat(flatFileWebSessionCache.getIfPresent(existingKey)).isNotNull();
  }

  @Test
  public void getTest() throws Exception {
    class ValueLoader implements Callable<Val> {
      @Override
      public Val call() throws Exception {
        return null;
      }
    }
    assertThat(flatFileWebSessionCache.get(existingKey, new ValueLoader()))
        .isNull();

    loadExistingKeyToCacheDir();
    assertThat(flatFileWebSessionCache.get(existingKey, new ValueLoader()))
        .isNotNull();
  }

  @Test(expected = ExecutionException.class)
  public void getTestCallableThrowsException() throws Exception {
    class ValueLoader implements Callable<Val> {
      @Override
      public Val call() throws Exception {
        throw new Exception();
      }
    }
    assertThat(flatFileWebSessionCache.get(existingKey, new ValueLoader()))
        .isNull();
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
    Path fileToDelete = Files.createFile(dir.resolve(key));
    assertThat(Files.exists(fileToDelete));
    flatFileWebSessionCache.invalidate(key);
    assertThat(Files.notExists(fileToDelete));
  }

  @Test
  public void invalidateTestObjectNotString() throws Exception {
    flatFileWebSessionCache.invalidate(new Object());
  }

  @Test
  public void putTest() throws Exception {
    loadExistingKeyToCacheDir();
    Val val = flatFileWebSessionCache.getIfPresent(existingKey);
    String newKey = "abcde12345";
    flatFileWebSessionCache.put(newKey, val);
    assertThat(flatFileWebSessionCache.getIfPresent(newKey)).isNotNull();
  }

  @Test
  public void putAllTest() throws Exception {
    loadExistingKeyToCacheDir();
    Val val = flatFileWebSessionCache.getIfPresent(existingKey);
    String newKey = "abcde12345";
    Map<String, Val> sessions = new HashMap<>();
    sessions.put(newKey, val);
    flatFileWebSessionCache.putAll(sessions);
    assertThat(flatFileWebSessionCache.asMap()).containsKey(newKey);
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

  @After
  public void tearDown() throws Exception {
    if (isDirEmpty(dir)) {
      Files.deleteIfExists(dir);
    } else {
      emptyAndDelete(dir);
    }
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
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc)
          throws IOException {
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

  private void loadExistingKeyToCacheDir() throws IOException {
    InputStream in = loadFile(existingKey);
    Path target = dir.resolve(existingKey);
    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
  }

  private InputStream loadFile(String file) {
    return this.getClass().getResourceAsStream("/" + file);
  }

  private void createOutdatedCacheFiles() throws IOException {
    DateTime threshold = new DateTime().minus(MAX_AGE_MS * 3);
    FileTime time = FileTime.fromMillis(threshold.getMillis());
    for (int i = 0; i < 10; i++) {
      Path path = dir.resolve(UUID.randomUUID().toString());
      Files.createFile(path);
      Files.setAttribute(path, "basic:lastAccessTime", time);
    }
  }
}