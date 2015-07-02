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
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.config.ConfigUtil;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSessionCacheCleaner.CleanerLifecycle;

import org.eclipse.jgit.lib.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Module extends LifecycleModule {
  private static final long MAX_AGE_MS = HOURS.toMillis(12);
  @Override
  protected void configure() {
    listener().to(CleanerLifecycle.class);
  }

  @Provides
  @Singleton
  @WebSessionDir
  Path getWebSessionDir(SitePaths site, PluginConfigFactory cfg,
      @PluginName String pluginName) {
    return Paths.get(cfg.getFromGerritConfig(pluginName).getString(
        "directory", site.site_path + "/websessions"));
  }

  @Provides
  @Singleton
  @WebSessionMaxAge
  long getMaxAgeWebSession(@GerritServerConfig Config gerritConfig) {
    return ConfigUtil.getTimeUnit(gerritConfig, "cache", "web_sessions",
        "maxAge", MAX_AGE_MS, MILLISECONDS);
  }

}