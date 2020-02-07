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
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledFuture;

@Singleton
class FlatFileWebSessionCacheCleaner implements LifecycleListener {

  private final WorkQueue queue;
  private final Provider<CleanupTask> cleanupTaskProvider;
  private final long cleanupIntervalMillis;
  private ScheduledFuture<?> scheduledCleanupTask;

  static class CleanupTask implements Runnable {
    private static final FluentLogger log = FluentLogger.forEnclosingClass();
    private final FlatFileWebSessionCache flatFileWebSessionCache;
    private final String pluginName;

    @Inject
    CleanupTask(FlatFileWebSessionCache flatFileWebSessionCache, @PluginName String pluginName) {
      this.flatFileWebSessionCache = flatFileWebSessionCache;
      this.pluginName = pluginName;
    }

    @Override
    public void run() {
      log.atInfo().log("Cleaning up expired file based websessions...");
      try {
        flatFileWebSessionCache.cleanUp();
      } catch (Exception e) {
        // log and do not prevent subsequent scheduled tasks from running
        // see https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledThreadPoolExecutor.html#scheduleAtFixedRate(java.lang.Runnable,%20long,%20long,%20java.util.concurrent.TimeUnit)
        log.atWarning().withCause(e).log("Exception during cleaning sessions");
      }
      log.atInfo().log("Cleaning up expired file based websessions...Done");
    }

    @Override
    public String toString() {
      return String.format("[%s] Clean up expired file based websessions", pluginName);
    }
  }

  @Inject
  FlatFileWebSessionCacheCleaner(
      WorkQueue queue,
      Provider<CleanupTask> cleanupTaskProvider,
      @CleanupInterval long cleanupInterval) {
    this.queue = queue;
    this.cleanupTaskProvider = cleanupTaskProvider;
    this.cleanupIntervalMillis = cleanupInterval;
  }

  @Override
  public void start() {
    scheduledCleanupTask =
        queue
            .getDefaultQueue()
            .scheduleAtFixedRate(
                cleanupTaskProvider.get(),
                SECONDS.toMillis(1),
                cleanupIntervalMillis,
                MILLISECONDS);
  }

  @Override
  public void stop() {
    if (scheduledCleanupTask != null) {
      scheduledCleanupTask.cancel(true);
      scheduledCleanupTask = null;
    }
  }
}
