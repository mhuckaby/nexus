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

import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

public class Hc4ProviderImplTest
    extends TestSupport
{
    private Hc4ProviderImpl testSubject;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private RemoteStorageContext globalRemoteStorageContext;

    @Before
    public void prepare()
    {
        when( globalRemoteStorageContext.getRemoteConnectionSettings() ).thenReturn(
            new DefaultRemoteConnectionSettings() );
        when( globalRemoteStorageContext.getRemoteProxySettings() ).thenReturn( new DefaultRemoteProxySettings() );
        when( applicationConfiguration.getGlobalRemoteStorageContext() ).thenReturn( globalRemoteStorageContext );
    }

    @Test
    public void sharedInstanceConfigurationTest()
    {
        testSubject = new Hc4ProviderImpl( applicationConfiguration, createMockParameters() );
        testSubject.configure();

        final HttpClient client = testSubject.createHttpClient();
        // Note: shared instance is shared across Nexus instance. It does not features connection pooling as
        // connections are
        // never reused intentionally

        // shared client does not reuse connections (no pool)
        Assert.assertTrue( ( (DefaultHttpClient) client ).getConnectionReuseStrategy() instanceof NoConnectionReuseStrategy );
        Assert.assertTrue( ( (DefaultHttpClient) client ).getConnectionManager() instanceof ManagedByOtherClientConnectionManager );
        // check is all set as needed
        Assert.assertEquals( 1234L, client.getParams().getLongParameter( ClientPNames.CONN_MANAGER_TIMEOUT, 0 ) );
        Assert.assertEquals( 1234, client.getParams().getIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT, 0 ) );
        Assert.assertEquals( 1234, client.getParams().getIntParameter( HttpConnectionParams.SO_TIMEOUT, 0 ) );
        Assert.assertEquals(
            1234,
            ( (PoolingClientConnectionManager) ( (EvictingClientConnectionManager) ( (ManagedByOtherClientConnectionManager) client.getConnectionManager() ).getDelegate() ).getDelegate() ).getMaxTotal() );
        Assert.assertEquals(
            1234,
            ( (PoolingClientConnectionManager) ( (EvictingClientConnectionManager) ( (ManagedByOtherClientConnectionManager) client.getConnectionManager() ).getDelegate() ).getDelegate() ).getDefaultMaxPerRoute() );
    }

    @Test
    public void createdInstanceConfigurationTest()
    {
        testSubject = new Hc4ProviderImpl( applicationConfiguration, createMockParameters() );
        testSubject.configure();

        // Note: explicitly created instance (like in case of proxies), it does pool and
        // returns customized client

        // we will reuse the "global" one, but this case is treated differently anyway by Hc4Provider
        final HttpClient client =
            testSubject.createHttpClient( applicationConfiguration.getGlobalRemoteStorageContext() );
        // shared client does reuse connections (does pool)
        Assert.assertTrue( ( (DefaultHttpClient) client ).getConnectionReuseStrategy() instanceof DefaultConnectionReuseStrategy );
        Assert.assertTrue( ( (DefaultHttpClient) client ).getConnectionManager() instanceof ManagedByOtherClientConnectionManager );
        // check is all set as needed
        Assert.assertEquals( 1234L, client.getParams().getLongParameter( ClientPNames.CONN_MANAGER_TIMEOUT, 0 ) );
        Assert.assertEquals( 1234, client.getParams().getIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT, 0 ) );
        Assert.assertEquals( 1234, client.getParams().getIntParameter( HttpConnectionParams.SO_TIMEOUT, 0 ) );
        final PoolingClientConnectionManager realConnMgr =
            (PoolingClientConnectionManager) ( (EvictingClientConnectionManager) ( (ManagedByOtherClientConnectionManager) client.getConnectionManager() ).getDelegate() ).getDelegate();
        Assert.assertEquals( 1234, realConnMgr.getMaxTotal() );
        Assert.assertEquals( 1234, realConnMgr.getDefaultMaxPerRoute() );
        client.getConnectionManager().shutdown();
    }

    // ==

    protected Hc4Parameters createMockParameters()
    {
        final Hc4Parameters result =
            (Hc4Parameters) Proxy.newProxyInstance( getClass().getClassLoader(), new Class[] { Hc4Parameters.class },
                new InvocationHandler()
                {
                    @Override
                    public Object invoke( Object instance, Method method, Object[] args )
                        throws Throwable
                    {
                        if ( method.getName().equals( "getConnectionPoolKeepalive" )
                            || method.getName().equals( "getConnectionPoolTimeout" ) )
                        {
                            return 1234L;
                        }
                        else
                        {
                            return 1234;
                        }
                    }
                } );
        return result;
    }
}
