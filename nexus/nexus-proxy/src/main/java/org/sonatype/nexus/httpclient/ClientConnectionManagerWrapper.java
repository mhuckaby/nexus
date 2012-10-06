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

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;

import com.google.common.base.Preconditions;

/**
 * A wrapping {@link ClientConnectionManager}, handy base class for {@link ClientConnectionManager} that needs some
 * extra stuff around delegated one.
 * 
 * @author cstamas
 * @since 2.2
 */
abstract class ClientConnectionManagerWrapper
    implements ClientConnectionManager
{
    private final ClientConnectionManager delegate;

    public ClientConnectionManagerWrapper( final ClientConnectionManager clientConnectionManager )
    {
        this.delegate = Preconditions.checkNotNull( clientConnectionManager );
    }

    public ClientConnectionManager getDelegate()
    {
        return delegate;
    }

    public SchemeRegistry getSchemeRegistry()
    {
        return getDelegate().getSchemeRegistry();
    }

    public ClientConnectionRequest requestConnection( HttpRoute route, Object state )
    {
        return getDelegate().requestConnection( route, state );
    }

    public void releaseConnection( ManagedClientConnection conn, long validDuration, TimeUnit timeUnit )
    {
        getDelegate().releaseConnection( conn, validDuration, timeUnit );
    }

    public void closeIdleConnections( long idletime, TimeUnit tunit )
    {
        getDelegate().closeIdleConnections( idletime, tunit );
    }

    public void closeExpiredConnections()
    {
        getDelegate().closeExpiredConnections();
    }

    public void shutdown()
    {
        getDelegate().shutdown();
    }
}
