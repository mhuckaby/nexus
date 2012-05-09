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

Ext.form.TimestampDisplayField = Ext.extend(Ext.form.DisplayField, {
      setValue : function(v) {
        // java give the timestamp in miliseconds, extjs consumes it in seconds
        var toSecs = Math.round(v / 1000);
        v = new Date.parseDate(toSecs, 'U').toString();
        this.setRawValue(v);
        return this;
      }
    });

Ext.reg('timestampDisplayField', Ext.form.TimestampDisplayField);
