FlatFile WebSession Plugin Configuration
========================================

This plugin replaces the built-in Gerrit H2 based websession
cache with a flatfile based implementation.  One reason to
use this plugin is to allow multiple gerrit servers to share
the same user websessions.  Sharing websessions is likely only
useful for multi-master Gerrit setups.  In order for multiple
Gerrit masters to share these websessions, the websessions
directory must be stored on a filesystem that is shared amongst
all the masters in your cluster.  The location of this directory
can be configured by adding an entry to the main gerrit config
file: $site_dir/etc/gerrit.config.  This location defaults to
$site_dir/websessions.

  [plugin "websession-flatfile"]
    directory = <disk_cache_directory>
  # NOTE: <disk_cache_directory> can be any location on the
  # shared filesystem that can be accessed by all servers,
  # and in which rename operations are atomic and allow
  # overwriting of existing files
```

For more information about sharing websessions in multi-master
setups, see the doucmentation for the multi-master plugin.

Reload the plugin on each master for the changes to take
effect.