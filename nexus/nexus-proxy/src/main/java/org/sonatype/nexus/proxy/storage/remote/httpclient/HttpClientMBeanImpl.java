/**
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

package org.sonatype.nexus.proxy.storage.remote.httpclient;

import org.apache.http.impl.conn.PoolingClientConnectionManager;

import javax.management.StandardMBean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PoolingClientConnectionManagerMBean} implementation.
 *
 * @since 2.2
 */
public class HttpClientMBeanImpl
    extends StandardMBean
    implements PoolingClientConnectionManagerMBean
{
    private final PoolingClientConnectionManager connectionManager;

    public HttpClientMBeanImpl(final PoolingClientConnectionManager connectionManager) {
        super(PoolingClientConnectionManagerMBean.class, false);
        this.connectionManager = checkNotNull(connectionManager);
    }

    @Override
    public int getMaxTotal() {
        return connectionManager.getMaxTotal();
    }

    @Override
    public int getDefaultMaxPerRoute() {
        return connectionManager.getDefaultMaxPerRoute();
    }

    @Override
    public int getLeased() {
        return connectionManager.getTotalStats().getLeased();
    }

    @Override
    public int getPending() {
        return connectionManager.getTotalStats().getPending();
    }

    @Override
    public int getAvailable() {
        return connectionManager.getTotalStats().getAvailable();
    }

    @Override
    public int getMax() {
        return connectionManager.getTotalStats().getMax();
    }
}
