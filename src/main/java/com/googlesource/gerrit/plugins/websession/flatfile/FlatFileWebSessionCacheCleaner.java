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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledFuture;

@Singleton
class FlatFileWebSessionCacheCleaner implements LifecycleListener {
  private static final int INITIAL_DELAY_MS = 1000;
  private final WorkQueue queue;
  private final Provider<CleanupTask> cleanupTaskProvider;
  private final long cleanupInterval;
  private ScheduledFuture<?> scheduledCleanupTask;

  @Inject
  FlatFileWebSessionCacheCleaner(
      WorkQueue queue,
      Provider<CleanupTask> cleanupTaskProvider,
      @CleanupInterval long cleanupInterval) {
    this.queue = queue;
    this.cleanupTaskProvider = cleanupTaskProvider;
    this.cleanupInterval = cleanupInterval;
  }

  @Override
  public void start() {
    scheduledCleanupTask =
        queue
            .getDefaultQueue()
            .scheduleAtFixedRate(
                cleanupTaskProvider.get(), INITIAL_DELAY_MS, cleanupInterval, MILLISECONDS);
  }

  @Override
  public void stop() {
    if (scheduledCleanupTask != null) {
      scheduledCleanupTask.cancel(true);
      scheduledCleanupTask = null;
    }
  }
}

class CleanupTask implements Runnable {
  private final FlatFileWebSessionCache flatFileWebSessionCache;
  private final String pluginName;

  @Inject
  CleanupTask(FlatFileWebSessionCache flatFileWebSessionCache, @PluginName String pluginName) {
    this.flatFileWebSessionCache = flatFileWebSessionCache;
    this.pluginName = pluginName;
  }

  @Override
  public void run() {
    flatFileWebSessionCache.cleanUp();
  }

  @Override
  public String toString() {
    return String.format("[%s] Clean up expired file based websessions", pluginName);
  }
}
