include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

PROVIDED_DEPS = [
  '//lib/joda:joda-time',
]

TEST_DEPS = GERRIT_PLUGIN_API + [
  ':websession-flatfile__plugin',
  '//lib:truth',
]

gerrit_plugin(
  name = 'websession-flatfile',
  srcs = SOURCES,
  resources = RESOURCES,
  manifest_entries = [
    'Gerrit-PluginName: websession-flatfile',
    'Gerrit-Module: com.googlesource.gerrit.plugins.websession.flatfile.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSession$Module',
    'Implementation-Title: Flat file WebSession',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/websession-flatfile',
  ],
  provided_deps = PROVIDED_DEPS,
)

java_library(
  name = 'classpath',
  deps = list(set(PROVIDED_DEPS) | set(TEST_DEPS))
)

java_sources(
  name = 'websession-flatfile-sources',
  srcs = SOURCES + RESOURCES,
)

java_test(
  name = 'websession-flatfile_tests',
  srcs = glob(['src/test/java/**/*.java']),
  resources = glob(['src/test/resources/**/']),
  labels = ['websession-flatfile'],
  source_under_test = [':websession-flatfile__plugin'],
  deps = TEST_DEPS,
)
