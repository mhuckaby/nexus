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
package org.sonatype.nexus.httpclient;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.util.SystemPropertiesHelper;

/**
 * Simple parameters implementation that sources from System properties (the overrides, set by user), otherwise it
 * serves up some sensible defaults. Currently, only the {@link #getConnectionTimeout(RemoteStorageContext)} parameter
 * comes from Nexus configuration (is contained in model, exposed over UI for users), all the other parameters might
 * come from JVM System Properties only. TODO: Reconsider the names of parameters: "poolXXX" suggests there is a pool
 * involved. While it might be, it is merely limiting the connection count (max and per route), as in case of shared
 * connection manager, pooling is intentionally shut down!
 * 
 * @author cstamas
 * @since 2.2
 */
@Singleton
@Named
public class Hc4ParametersImpl
    implements Hc4Parameters
{
    /**
     * Key for customizing connection pool maximum size. Value should be integer equal to 0 or greater. Pool size of 0
     * will actually prevent use of pool. Any positive number means the actual size of the pool to be created. This is a
     * hard limit, connection pool will never contain more than this count of open sockets.
     */
    private static final String CONNECTION_POOL_MAX_SIZE_KEY = "nexus.apacheHttpClient4x.connectionPoolMaxSize";

    /**
     * Default pool max size: 200.
     */
    private static final int CONNECTION_POOL_MAX_SIZE_DEFAULT = 200;

    /**
     * Key for customizing connection pool size per route (usually per-repository, but not quite in case of Mirrors).
     * Value should be integer equal to 0 or greater. Pool size of 0 will actually prevent use of pool. Any positive
     * number means the actual size of the pool to be created.
     */
    private static final String CONNECTION_POOL_SIZE_KEY = "nexus.apacheHttpClient4x.connectionPoolSize";

    /**
     * Default pool size: 20.
     */
    private static final int CONNECTION_POOL_SIZE_DEFAULT = 20;

    /**
     * Key for customizing connection pool keep-alive. In other words, how long open connections (sockets) are kept in
     * pool before evicted and closed. Value is milliseconds.
     */
    private static final String CONNECTION_POOL_KEEPALIVE_KEY = "nexus.apacheHttpClient4x.connectionPoolKeepalive";

    /**
     * Default pool keep-alive: 1 minute.
     */
    private static final long CONNECTION_POOL_KEEPALIVE_DEFAULT = TimeUnit.MINUTES.toMillis( 1 );

    /**
     * Key for customizing connection pool timeout. In other words, how long should a HTTP request execution be blocked
     * when pool is depleted, for a connection. Value is milliseconds.
     */
    private static final String CONNECTION_POOL_TIMEOUT_KEY = "nexus.apacheHttpClient4x.connectionPoolTimeout";

    /**
     * Default pool timeout: equals to {@link #CONNECTION_POOL_KEEPALIVE_DEFAULT}.
     */
    private static final long CONNECTION_POOL_TIMEOUT_DEFAULT = CONNECTION_POOL_KEEPALIVE_DEFAULT;

    @Override
    public int getConnectionPoolMaxSize( final RemoteStorageContext context )
    {
        return SystemPropertiesHelper.getInteger( CONNECTION_POOL_MAX_SIZE_KEY, CONNECTION_POOL_MAX_SIZE_DEFAULT );
    }

    @Override
    public int getConnectionPoolSize( final RemoteStorageContext context )
    {
        return SystemPropertiesHelper.getInteger( CONNECTION_POOL_SIZE_KEY, CONNECTION_POOL_SIZE_DEFAULT );
    }

    @Override
    public long getConnectionPoolKeepalive( final RemoteStorageContext context )
    {
        return SystemPropertiesHelper.getLong( CONNECTION_POOL_KEEPALIVE_KEY, CONNECTION_POOL_KEEPALIVE_DEFAULT );
    }

    @Override
    public long getConnectionPoolTimeout( final RemoteStorageContext context )
    {
        return SystemPropertiesHelper.getLong( CONNECTION_POOL_TIMEOUT_KEY, CONNECTION_POOL_TIMEOUT_DEFAULT );
    }

    @Override
    public int getConnectionTimeout( final RemoteStorageContext context )
    {
        if ( context.getRemoteConnectionSettings() != null )
        {
            return context.getRemoteConnectionSettings().getConnectionTimeout();
        }
        else
        {
            // see DefaultRemoteConnectionSetting
            return 1000;
        }
    }

    @Override
    public int getSoTimeout( final RemoteStorageContext context )
    {
        // this parameter is actually set from #getConnectionTimeout
        return getConnectionTimeout( context );
    }
}
