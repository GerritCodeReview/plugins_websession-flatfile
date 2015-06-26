Flat File WebSession Plugin Configuration
=========================================

In order for multiple Gerrit masters to share websessions, the
websessions directory must be stored on a filesystem that is
shared amongst all the masters in your cluster.  The location
of this directory can be configured by adding an entry to the
main Gerrit config file: $site_dir/etc/gerrit.config.  This
location defaults to $site_dir/websessions.

  [plugin "@PLUGIN@"]
    directory = <disk_cache_directory>
  # NOTE: <disk_cache_directory> can be any location on the
  # shared filesystem that can be accessed by all servers,
  # and in which rename operations are atomic and allow
  # overwriting of existing files
```

Reload the plugin on each master for the changes to take
effect.

SEE ALSO
--------

For more information about sharing websessions in multi-master
setups, see the documentation for the
[multi-master plugin](https://gerrit-review.googlesource.com/#/admin/projects/plugins/multi-master).
