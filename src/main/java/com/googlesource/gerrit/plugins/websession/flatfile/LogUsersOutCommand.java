// Copyright (C) 2019 The Android Open Source Project
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

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.CapabilityScope;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@RequiresCapability(value = GlobalCapability.FLUSH_CACHES, scope = CapabilityScope.CORE)
@CommandMetaData(name = "log-users-out", description = "Logs specific users out or all of them")
public final class LogUsersOutCommand extends SshCommand {

  @Argument(
      usage =
          "User information to find: LastName,\\ Firstname,  email@address.com, account id or an user name.  Be sure to double-escape spaces, for example: \"log-users-out Last,\\\\ First\"")
  private String name = "";

  @Option(name = "--all", usage = "Clears web sessions for all users")
  private boolean all = false;

  private final AccountResolver accountResolver;
  private final IdentifiedUser.GenericFactory userFactory;
  private final AccountCache accountCache;
  private final FlatFileWebSessionsCache flatFileWebSessionsCache;

  @Inject
  LogUsersOutCommand(
      AccountResolver accountResolver,
      IdentifiedUser.GenericFactory userFactory,
      AccountCache accountCache,
      FlatFileWebSessionsCache flatFileWebSessionsCache) {
    this.accountResolver = accountResolver;
    this.userFactory = userFactory;
    this.accountCache = accountCache;
    this.flatFileWebSessionsCache = flatFileWebSessionsCache;
  }

  @Override
  public void run() throws UnloggedFailure, Exception {
    Account account;

    if (name.isEmpty()) {
      throw new UnloggedFailure(
          1,
          "You need to tell me who to find:  LastName,\\\\ Firstname, email@address.com, account id or an user name.  "
              + "Be sure to double-escape spaces, for example: \"log-users-out Last,\\\\ First\"");
    }
    Set<Account.Id> idList = accountResolver.findAll(name);
    if (idList.isEmpty()) {
      throw new UnloggedFailure(
          1,
          "No accounts found for your query: \""
              + name
              + "\""
              + " Tip: Try double-escaping spaces, for example: \"log-users-out Last,\\\\ First\"");
    }


    for (Account.Id id : idList) {
      account = accountResolver.find(id.toString());
      if (account == null) {
        throw new UnloggedFailure("Account " + id.toString() + " does not exist.");
      }

      if (!all) {
        flatFileWebSessionsCache.invalidateSpecificUser(id);
      } else {
        flatFileWebSessionsCache.invalidateAll();
      }
    }
  }
}
