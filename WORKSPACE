workspace(name = "websession_flatfile")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "b6120a9fa50945d38f0a4d55d5879e3ec465c5e5",
    shallow_since = "1701477032 -0700",
)

load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Load release Plugin API
gerrit_api()

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
