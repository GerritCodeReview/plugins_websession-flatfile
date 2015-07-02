gerrit_plugin(
  name = 'websession-flatfile',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: websession-flatfile',
    'Gerrit-Module: com.googlesource.gerrit.plugins.websession.flatfile.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSession$Module',
    'Implementation-Title: Flat file WebSession',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/websession-flatfile',
  ],
  provided_deps = [
    '//lib/joda:joda-time',
  ],
)

java_test(
  name = 'websession-flatfile_tests',
  srcs = glob(['src/test/java/**/*.java']),
  resources = glob(['src/test/resources/**/']),
  labels = ['websession-flatfile'],
  source_under_test = [':websession-flatfile__plugin'],
  deps = [
    ':websession-flatfile__plugin',
    '//gerrit-httpd:httpd',
    '//lib:guava',
    '//lib:junit',
    '//lib:truth',
  ],
)
