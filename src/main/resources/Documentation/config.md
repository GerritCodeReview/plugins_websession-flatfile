Flat File WebSession Plugin Configuration
=========================================

In order for multiple Gerrit masters to share websessions, the
websessions directory must be stored on a filesystem that is
shared amongst all the masters in your cluster.  The location
of this directory can be configured by adding an entry to the
main Gerrit config file: $site_dir/etc/gerrit.config.  This
location defaults to $site_dir/websessions.

```
  [plugin "@PLUGIN@"]
    directory = <disk_cache_directory>

  # NOTE: <disk_cache_directory> can be any location on the
  # shared filesystem that can be accessed by all servers,
  # and in which rename operations are atomic and allow
  # overwriting of existing files
```

Reload the plugin on each master for the changes to take
effect.

The plugin periodically cleans up the cache directory, deleting
files corresponding to expired sessions. The frequency of this
operation can be specified in the configuration. For example:

```
  [plugin "@PLUGIN@"]
    cleanupInterval = 1h
```

indicates the cleanup operation to be triggered every hour.

Values should use common time unit suffixes to express their setting:

* h, hr, hour, hours
* d, day, days
* w, week, weeks (`1 week` is treated as `7 days`)
* mon, month, months (`1 month` is treated as `30 days`)
* y, year, years (`1 year` is treated as `365 days`)

If a time unit suffix is not specified, `hours` is assumed.

Time intervals smaller than one hour are not supported.

If 'cleanupInterval' is not present in the configuration, the
cleanup operation is triggered every 24 hours.


SEE ALSO
--------

For more information about sharing websessions in multi-master
setups, see the documentation for the
[multi-master plugin](https://gerrit-review.googlesource.com/#/admin/projects/plugins/multi-master).
