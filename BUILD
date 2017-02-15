load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
    name = "websession-flatfile",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: websession-flatfile",
        "Gerrit-Module: com.googlesource.gerrit.plugins.websession.flatfile.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSession$Module",
        "Implementation-Title: Flat file WebSession",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/websession-flatfile",
    ],
)

junit_tests(
    name = "websession_flatfile_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["websession-flatfile"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":websession-flatfile__plugin",
    ],
    local = 1,
)
