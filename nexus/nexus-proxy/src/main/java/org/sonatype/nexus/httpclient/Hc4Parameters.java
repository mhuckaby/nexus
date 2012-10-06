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

import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * Hc4 parameters source.
 * 
 * @author cstamas
 * @since 2.2
 */
public interface Hc4Parameters
{
    /**
     * Returns the pool max size.
     * 
     * @param context
     * @return pool max size
     */
    int getConnectionPoolMaxSize( final RemoteStorageContext context );

    /**
     * Returns the pool size per route.
     * 
     * @param context
     * @return pool per route sizw
     */
    int getConnectionPoolSize( final RemoteStorageContext context );

    /**
     * Returns the keep alive (idle open) time in milliseconds.
     * 
     * @param context
     * @return keep alive in milliseconds.
     */
    long getConnectionPoolKeepalive( final RemoteStorageContext context );

    /**
     * Returns the pool timeout in milliseconds.
     * 
     * @param context
     * @return pool timeout in milliseconds.
     */
    long getConnectionPoolTimeout( final RemoteStorageContext context );

    /**
     * Returns the connection timeout in milliseconds. The timeout until connection is established.
     * 
     * @param context
     * @return the connection timeout in milliseconds.
     */
    int getConnectionTimeout( final RemoteStorageContext context );

    /**
     * Returns the SO_SOCKET timeout in milliseconds. The timeout for waiting for data on established connection.
     * 
     * @param context
     * @return the SO_SOCKET timeout in milliseconds.
     */
    int getSoTimeout( final RemoteStorageContext context );
}
