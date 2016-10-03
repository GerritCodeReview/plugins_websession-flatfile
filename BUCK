include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

DEPS = GERRIT_PLUGIN_API + [
  ':websession-flatfile__plugin',
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
)

java_library(
  name = 'classpath',
  deps = DEPS + GERRIT_TESTS,
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
  deps = GERRIT_TESTS + DEPS,
  source_under_test = [':websession-flatfile__plugin'],
)
