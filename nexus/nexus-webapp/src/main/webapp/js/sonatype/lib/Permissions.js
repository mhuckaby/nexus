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

  Ext.namespace('Sonatype.lib');

  Sonatype.lib.Permissions = {
    READ : 1, // 0001
    EDIT : 2, // 0010
    DELETE : 4, // 0100
    CREATE : 8, // 1000
    ALL : 15, // 1111
    NONE : 0, // 0000

    // returns bool indicating if value has all perms
    // all values are base 10 representations of the n-bit representation
    // Example: for 4-bit permissions: 3 (base to) represents 0011 (base 2)
    checkPermission : function(value, perm /* , perm... */) {
      var p = perm;

      if (Sonatype.user.curr.repoServer)
      {
        Ext.each(Sonatype.user.curr.repoServer, function(item, i, arr) {
              if (item.id == value)
              {
                value = item.value;
                return false;
              }
            });
      }

      if (arguments.length > 2)
      {
        var perms = Array.slice(arguments, 2);
        Ext.each(perms, function(item, i, arr) {
              p = p | item;
            });
      }

      return ((p & value) == p);
    }
  };

})();