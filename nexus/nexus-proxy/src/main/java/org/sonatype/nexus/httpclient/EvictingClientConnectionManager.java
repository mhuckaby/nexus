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

import org.apache.http.conn.ClientConnectionManager;

/**
 * A wrapping {@link ClientConnectionManager} that starts a background monitoring thread to perform the eviction. The
 * thread is set as daemon, so it will not prevent JVM shutdown, but it's still recommended to cleanly shut down the
 * connection manager using {@link ClientConnectionManager#shutdown()} method, that will cleanly stop the thread too.
 * 
 * @author cstamas
 * @since 2.2
 */
class EvictingClientConnectionManager
    extends ClientConnectionManagerWrapper
{
    private final EvictingThread evictingThread;

    public EvictingClientConnectionManager( final ClientConnectionManager clientConnectionManager, final long keepAlive )
    {
        super( clientConnectionManager );
        this.evictingThread = new EvictingThread( clientConnectionManager, keepAlive );
        this.evictingThread.start();
    }

    public void shutdown()
    {
        evictingThread.shutdown();
        super.shutdown();
    }
}
