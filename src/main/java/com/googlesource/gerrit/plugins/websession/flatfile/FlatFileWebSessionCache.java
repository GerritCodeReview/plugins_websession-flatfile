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
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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

  private final File dir;

  @Inject
  public FlatFileWebSessionCache(SitePaths site, PluginConfigFactory cfg) {
    dir = new File(cfg.getFromGerritConfig("websession-flatfile")
        .getString("directory", site.site_path + "/websessions"));
    if (!dir.exists()) {
      log.info(dir + " not found. Creating it.");
      dir.mkdir();
    }
  }

  @Override
  public ConcurrentMap<String, Val> asMap() {
    ConcurrentMap<String, Val> map = new ConcurrentHashMap<>();
    File[] files = dir.listFiles();
    if (files == null) {
      return map;
    }
    for (File f : files) {
      Val v = readFile(f);
      if (v != null) {
        map.put(f.getName(), v);
      }
    }
    return map;
  }

  @Override
  public void cleanUp() {
    // do nothing
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
    ImmutableMap.Builder<String, Val> mapBuilder =
        new ImmutableMap.Builder<>();
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
      File f = new File(dir, (String) key);
      return readFile(f);
    }
    return null;
  }

  @Override
  public void invalidate(Object key) {
    if (key instanceof String) {
      deleteFile(new File(dir, (String) key));
    }
  }

  @Override
  public void invalidateAll() {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        deleteFile(f);
      }
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
    File tempFile = null;
    OutputStream fileStream = null;
    ObjectOutputStream objStream = null;

    try {
      tempFile = File.createTempFile(UUID.randomUUID().toString(), null, dir);
      fileStream = new FileOutputStream(tempFile);

      objStream = new ObjectOutputStream(fileStream);
      objStream.writeObject(value);

      File f = new File(dir, key);
      if (!tempFile.renameTo(f)) {
        log.warn("Cannot put into cache " + dir.getAbsolutePath()
            + "; error renaming temp file");
      }
    } catch (FileNotFoundException e) {
      log.warn("Cannot put into cache " + dir.getAbsolutePath(), e);
    } catch (IOException e) {
      log.warn("Cannot put into cache " + dir.getAbsolutePath(), e);
    } finally {
      if (tempFile != null) {
        deleteFile(tempFile);
      }
      close(fileStream);
      close(objStream);
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
    String[] files = dir.list();
    if (files == null) {
      return 0;
    }
    return files.length;
  }

  @Override
  public CacheStats stats() {
    log.warn("stats() unimplemented");
    return null;
  }

  private Val readFile(File f) {
    InputStream fileStream = null;
    ObjectInputStream objStream = null;
    try {
      fileStream = new FileInputStream(f);
      objStream = new ObjectInputStream(fileStream);
      try {
        return (Val) objStream.readObject();
      } catch (ClassNotFoundException e) {
        log.warn("Entry " + f.getName() + " in cache " + dir.getAbsolutePath()
            + " has an incompatible class and can't be deserialized. "
            + "Invalidating entry.");
        invalidate(f.getName());
      }
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
      log.warn("Cannot read cache " + dir.getAbsolutePath(), e);
    } finally {
      close(fileStream);
      close(objStream);
    }
    return null;
  }

  private void deleteFile(File f) {
    if (f.exists() && !f.delete()) {
      log.warn("Cannot delete file " + f.getName() + " from cache "
          + dir.getAbsolutePath());
    }
  }

  private void close(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        log.warn("Cannot close stream", e);
      }
    }
  }
}
