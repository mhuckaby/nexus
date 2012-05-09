/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

(function() {

  // Default anonymous user permissions; 3-bit permissions: delete | edit | read
  Sonatype.user.anon = {
    username : '',
    isLoggedIn : false,
    authToken : null,
    repoServer : {}
  };

  Sonatype.user.curr = Sonatype.utils.cloneObj(Sonatype.user.anon);
  // Sonatype.user.curr = {
  // repoServer : {
  // viewSearch : 1,
  // viewUpdatedArtifacts : 1,
  // viewCachedArtifacts : 1,
  // viewDeployedArtifacts : 1,
  // viewSystemChanges : 1,
  // maintRepos : 3,
  // maintLogs : 1,
  // maintConfig : 1,
  // configServer : 3,
  // configGroups : 7,
  // configRules : 7,
  // configRepos : 7
  // }
  // };

})();
