/*
 * SonarLint Language Server
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.ls.util.Utils;

import static java.util.Optional.ofNullable;

public class ScmIgnoredCache {
  private static final SonarLintLogger LOG = SonarLintLogger.get();

  private final SonarLintExtendedLanguageClient client;
  public final Map<URI, Optional<Boolean>> filesIgnoredByUri = new ConcurrentHashMap<>();

  public ScmIgnoredCache(SonarLintExtendedLanguageClient client) {
    this.client = client;
  }

  public void didClose(URI fileUri) {
    filesIgnoredByUri.remove(fileUri);
  }

  public Optional<Boolean> isIgnored(URI fileUri) {
    Optional<Boolean> isIgnored;
    try {
      isIgnored = getOrFetchAsync(fileUri).get(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Utils.interrupted(e);
      isIgnored = Optional.empty();
    } catch (Exception e) {
      LOG.warn("Unable to get SCM ignore status", e);
      isIgnored = Optional.empty();
    }
    return isIgnored;
  }

  private CompletableFuture<Optional<Boolean>> getOrFetchAsync(URI fileUri) {
    if (filesIgnoredByUri.containsKey(fileUri)) {
      return CompletableFuture.completedFuture(filesIgnoredByUri.get(fileUri));
    }
    return client.isIgnoredByScm(fileUri.toString())
      .handle((r, t) -> {
        if (t != null) {
          LOG.error("Unable to check if file " + fileUri + " is SCM ignored", t);
        }
        return r;
      })
      .thenApply(ignored -> {
        var ignoredOpt = ofNullable(ignored);
        filesIgnoredByUri.put(fileUri, ignoredOpt);
        LOG.debug("Cached SCM ignore status for file '{}': {}", fileUri, ignoredOpt.map(b -> Boolean.TRUE.equals(b) ? "Ignored" : "Not ignored").orElse("Unknown"));
        return ignoredOpt;
      });
  }

}
