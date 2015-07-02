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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;

class FlatFileWebSessionCacheCleaner implements Runnable {

  static class CleanerLifecycle implements LifecycleListener {
    private static final int INITIAL_DELAY_MS = 1000;
    private final WorkQueue queue;
    private final FlatFileWebSessionCacheCleaner cleaner;
    private final long cleanUpInterval;

    @Inject
    CleanerLifecycle(WorkQueue queue, FlatFileWebSessionCacheCleaner cleaner,
        @CleanUpInterval long cleanUpInterval) {
      this.queue = queue;
      this.cleaner = cleaner;
      this.cleanUpInterval = cleanUpInterval;
    }

    @Override
    public void start() {
      queue.getDefaultQueue().scheduleAtFixedRate(cleaner, INITIAL_DELAY_MS,
          cleanUpInterval, MILLISECONDS);
    }

    @Override
    public void stop() {
    }
  }

  private FlatFileWebSessionCache flatFileWebSessionCache;

  @Inject
  FlatFileWebSessionCacheCleaner(FlatFileWebSessionCache flatFileWebSessionCache) {
    this.flatFileWebSessionCache = flatFileWebSessionCache;
  }

  @Override
  public void run() {
    flatFileWebSessionCache.cleanUp();
  }

  @Override
  public String toString() {
    return "FlatFile WebSession Cleaner";
  }
}
