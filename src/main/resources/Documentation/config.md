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

### Cleanup gotchas

Sessions are cleaned up when their corresponding file was created longer than
`cache.web_sessions.maxAge` ago.

This is based on the _current_ value of `cache.web_sessions.maxAge`, rather than
its value _at the time_ the session was created.

If the value of `cache.web_sessions.maxAge` is then changed by a Gerrit admin,
then two different scenarios arise, depending on whether maxAge was increased
or decreased.

#### maxAge is increased

1. `cache.web_sessions.maxAge = 10 days`
2. *1st of January*: user Jane creates a session.
3. *1st of January*: Gerrit admin sets `cache.web_sessions.maxAge = 1 day` and
   restarts Gerrit.
4. *2nd of January*: Jane's session file is removed from disk.

Jane will need to acquire a new session, irrespective of the fact that her
session was _originally_ created when maxAge was set to `10 days`.

#### maxAge is decreased

1. `cache.web_sessions.maxAge = 1 day`
2. *1st of January*: user Jane creates a session.
3. *1st of January*: Gerrit admin sets `cache.web_sessions.maxAge = 10 days` and
   restarts Gerrit.
4. *2nd of January*: Jane's session is left on disk, until the *11th of January*

Note that, this has no effect on the session validity: the session _itself_ is
expired (i.e. its `expiresAt` value is still based on `maxAge = 1 day`) and thus
Jane will need acquire a new session as she would have, had `maxAge` not changed

SEE ALSO
--------

For more information about sharing websessions in multi-master
setups, see the documentation for the
[multi-master plugin](https://gerrit-review.googlesource.com/#/admin/projects/plugins/multi-master).
