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
package org.sonatype.nexus.apachehttpclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

import com.google.common.base.Preconditions;

/**
 * Default implementation of {@link Hc4Provider}.
 * 
 * @author cstamas
 * @since 2.2
 */
@Singleton
@Named
public class Hc4ProviderImpl
    extends AbstractLoggingComponent
    implements Hc4Provider, EventListener
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

    // ==

    /**
     * Application configuration holding the {@link GlobalRemoteConnectionSettings}.
     */
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * UA builder component.
     */
    private final UserAgentBuilder userAgentBuilder;

    /**
     * The low level core event multicaster.
     */
    private final ApplicationEventMulticaster applicationEventMulticaster;

    /**
     * Shared client connection manager.
     */
    private final PoolingClientConnectionManager sharedConnectionManager;

    /**
     * Thread evicting idle open connections from {@link #sharedConnectionManager}.
     */
    private final EvictingThread evictingThread;

    /**
     * Used to install created {@link PoolingClientConnectionManager} into jmx.
     */
    private final PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

    /**
     * Constructor.
     * 
     * @param applicationConfiguration the Nexus {@link ApplicationConfiguration}.
     * @param userAgentBuilder UA builder component.
     * @param multicaster the event multicaster
     */
    @Inject
    public Hc4ProviderImpl( final ApplicationConfiguration applicationConfiguration,
                            final UserAgentBuilder userAgentBuilder, final ApplicationEventMulticaster multicaster,
                            final PoolingClientConnectionManagerMBeanInstaller jmxInstaller )
    {
        this.applicationConfiguration = Preconditions.checkNotNull( applicationConfiguration );
        this.userAgentBuilder = Preconditions.checkNotNull( userAgentBuilder );
        this.jmxInstaller = Preconditions.checkNotNull( jmxInstaller );
        this.sharedConnectionManager = createClientConnectionManager();
        this.evictingThread = new EvictingThread( sharedConnectionManager, getConnectionPoolKeepalive() );
        this.evictingThread.start();
        this.applicationEventMulticaster = Preconditions.checkNotNull( multicaster );
        this.applicationEventMulticaster.addEventListener( this );
        this.jmxInstaller.register( sharedConnectionManager );
        getLogger().info( "{} started up (keep-alive {} millis), listening for shutdown.", getClass().getSimpleName(),
                          getConnectionPoolKeepalive() );
    }

    // configuration

    /**
     * Returns the pool max size.
     * 
     * @param context
     * @return pool max size
     */
    protected int getConnectionPoolMaxSize()
    {
        return SystemPropertiesHelper.getInteger( CONNECTION_POOL_MAX_SIZE_KEY, CONNECTION_POOL_MAX_SIZE_DEFAULT );
    }

    /**
     * Returns the pool size per route.
     * 
     * @param context
     * @return pool per route sizw
     */
    protected int getConnectionPoolSize()
    {
        return SystemPropertiesHelper.getInteger( CONNECTION_POOL_SIZE_KEY, CONNECTION_POOL_SIZE_DEFAULT );
    }

    /**
     * Returns the keep alive (idle open) time in milliseconds.
     * 
     * @param context
     * @return keep alive in milliseconds.
     */
    protected long getConnectionPoolKeepalive()
    {
        return SystemPropertiesHelper.getLong( CONNECTION_POOL_KEEPALIVE_KEY, CONNECTION_POOL_KEEPALIVE_DEFAULT );
    }

    /**
     * Returns the pool timeout in milliseconds.
     * 
     * @param context
     * @return pool timeout in milliseconds.
     */
    protected long getConnectionPoolTimeout()
    {
        return SystemPropertiesHelper.getLong( CONNECTION_POOL_TIMEOUT_KEY, CONNECTION_POOL_TIMEOUT_DEFAULT );
    }

    /**
     * Returns the connection timeout in milliseconds. The timeout until connection is established.
     * 
     * @param context
     * @return the connection timeout in milliseconds.
     */
    protected int getConnectionTimeout( final RemoteStorageContext context )
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

    /**
     * Returns the SO_SOCKET timeout in milliseconds. The timeout for waiting for data on established connection.
     * 
     * @param context
     * @return the SO_SOCKET timeout in milliseconds.
     */
    protected int getSoTimeout( final RemoteStorageContext context )
    {
        // this parameter is actually set from #getConnectionTimeout
        return getConnectionTimeout( context );
    }

    // ==

    /**
     * Performs a clean shutdown on this component, it kills the evicting thread and shuts down the shared connection
     * manager. Multiple invocation of this method is safe, it will not do anything.
     */
    public synchronized void shutdown()
    {
        evictingThread.interrupt();
        jmxInstaller.unregister();
        sharedConnectionManager.shutdown();
        applicationEventMulticaster.removeEventListener( this );
        getLogger().info( "{} shut down.", getClass().getSimpleName() );
    }

    // == EventListener API

    @Override
    public void onEvent( Event<?> evt )
    {
        if ( evt instanceof NexusStoppedEvent )
        {
            shutdown();
        }
    }

    // ==

    /**
     * Safety net to prevent thread leaks (in non-production environment, mainly for ITs or UTs).
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        try
        {
            shutdown();
        }
        finally
        {
            super.finalize();
        }
    }

    // == Hc4Provider API

    @Override
    public DefaultHttpClient createHttpClient()
    {
        final DefaultHttpClient result = createHttpClient( applicationConfiguration.getGlobalRemoteStorageContext() );
        // connection manager will cap the max count of connections, but with this below
        // we get rid of pooling. Pooling is used in Proxy repositories only, as all other
        // components using the "shared" httpClient should not produce hiw rate of requests
        // anyway, as they usually happen per user interactions (GPG gets keys are staging repo is closed, if not cached
        // yet, LVO gets info when UI's main window is loaded into user's browser, etc
        result.setReuseStrategy( new NoConnectionReuseStrategy() );
        return result;
    }

    @Override
    public DefaultHttpClient createHttpClient( final RemoteStorageContext context )
    {
        return createHttpClient( context, sharedConnectionManager );
    }

    // ==

    protected DefaultHttpClient createHttpClient( final RemoteStorageContext context,
                                                  final ClientConnectionManager clientConnectionManager )
    {
        final DefaultHttpClient httpClient =
            new DefaultHttpClient( clientConnectionManager, createHttpParams( context ) )
            {
                @Override
                protected BasicHttpProcessor createHttpProcessor()
                {
                    final BasicHttpProcessor result = super.createHttpProcessor();
                    result.addResponseInterceptor( new ResponseContentEncoding() );
                    return result;
                }
            };
        configureAuthentication( httpClient, context.getRemoteAuthenticationSettings(), null );
        configureProxy( httpClient, context.getRemoteProxySettings() );
        return httpClient;
    }

    protected HttpParams createHttpParams( final RemoteStorageContext context )
    {
        HttpParams params = new SyncBasicHttpParams();
        params.setParameter( HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1 );
        params.setBooleanParameter( HttpProtocolParams.USE_EXPECT_CONTINUE, false );
        params.setBooleanParameter( HttpConnectionParams.STALE_CONNECTION_CHECK, false );
        params.setIntParameter( HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024 );
        params.setLongParameter( ClientPNames.CONN_MANAGER_TIMEOUT, getConnectionPoolTimeout() );
        params.setIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT, getConnectionTimeout( context ) );
        params.setIntParameter( HttpConnectionParams.SO_TIMEOUT, getSoTimeout( context ) );
        params.setParameter( HttpProtocolParams.USER_AGENT, userAgentBuilder.formatGenericUserAgentString() );
        return params;
    }

    protected PoolingClientConnectionManager createClientConnectionManager()
        throws IllegalStateException
    {
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register( new Scheme( "http", 80, PlainSocketFactory.getSocketFactory() ) );
        schemeRegistry.register( new Scheme( "https", 443, SSLSocketFactory.getSocketFactory() ) );
        final PoolingClientConnectionManager connManager = new PoolingClientConnectionManager( schemeRegistry );

        final int maxConnectionCount = getConnectionPoolMaxSize();
        final int perRouteConnectionCount = Math.min( getConnectionPoolSize(), maxConnectionCount );

        connManager.setMaxTotal( maxConnectionCount );
        connManager.setDefaultMaxPerRoute( perRouteConnectionCount );

        return connManager;
    }

    // ==

    protected void configureAuthentication( final DefaultHttpClient httpClient, final RemoteAuthenticationSettings ras,
                                            final HttpHost proxyHost )
    {
        if ( ras != null )
        {
            String authScope = "target";
            if ( proxyHost != null )
            {
                authScope = proxyHost.toHostString() + " proxy";
            }

            List<String> authorisationPreference = new ArrayList<String>( 2 );
            authorisationPreference.add( AuthPolicy.DIGEST );
            authorisationPreference.add( AuthPolicy.BASIC );
            Credentials credentials = null;
            if ( ras instanceof ClientSSLRemoteAuthenticationSettings )
            {
                throw new IllegalArgumentException( "SSL client authentication not yet supported!" );
            }
            else if ( ras instanceof NtlmRemoteAuthenticationSettings )
            {
                final NtlmRemoteAuthenticationSettings nras = (NtlmRemoteAuthenticationSettings) ras;
                // Using NTLM auth, adding it as first in policies
                authorisationPreference.add( 0, AuthPolicy.NTLM );
                getLogger().info( "... {} authentication setup for NTLM domain '{}'", authScope, nras.getNtlmDomain() );
                credentials =
                    new NTCredentials( nras.getUsername(), nras.getPassword(), nras.getNtlmHost(), nras.getNtlmDomain() );
            }
            else if ( ras instanceof UsernamePasswordRemoteAuthenticationSettings )
            {
                final UsernamePasswordRemoteAuthenticationSettings uras =
                    (UsernamePasswordRemoteAuthenticationSettings) ras;
                getLogger().info( "... {} authentication setup for remote storage with username '{}'", authScope,
                    uras.getUsername() );
                credentials = new UsernamePasswordCredentials( uras.getUsername(), uras.getPassword() );
            }

            if ( credentials != null )
            {
                if ( proxyHost != null )
                {
                    httpClient.getCredentialsProvider().setCredentials( new AuthScope( proxyHost ), credentials );
                    httpClient.getParams().setParameter( AuthPNames.PROXY_AUTH_PREF, authorisationPreference );
                }
                else
                {
                    httpClient.getCredentialsProvider().setCredentials( AuthScope.ANY, credentials );
                    httpClient.getParams().setParameter( AuthPNames.TARGET_AUTH_PREF, authorisationPreference );
                }
            }
        }
    }

    protected void configureProxy( final DefaultHttpClient httpClient, final RemoteProxySettings remoteProxySettings )
    {
        if ( remoteProxySettings.isEnabled() )
        {
            getLogger().info( "... proxy setup with host '{}'", remoteProxySettings.getHostname() );

            final HttpHost proxy = new HttpHost( remoteProxySettings.getHostname(), remoteProxySettings.getPort() );
            httpClient.getParams().setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );

            // check if we have non-proxy hosts
            if ( remoteProxySettings.getNonProxyHosts() != null && !remoteProxySettings.getNonProxyHosts().isEmpty() )
            {
                final Set<Pattern> nonProxyHostPatterns =
                    new HashSet<Pattern>( remoteProxySettings.getNonProxyHosts().size() );
                for ( String nonProxyHostRegex : remoteProxySettings.getNonProxyHosts() )
                {
                    try
                    {
                        nonProxyHostPatterns.add( Pattern.compile( nonProxyHostRegex, Pattern.CASE_INSENSITIVE ) );
                    }
                    catch ( PatternSyntaxException e )
                    {
                        getLogger().warn( "Invalid non proxy host regex: {}", nonProxyHostRegex, e );
                    }
                }
                httpClient.setRoutePlanner( new NonProxyHostsAwareHttpRoutePlanner(
                    httpClient.getConnectionManager().getSchemeRegistry(), nonProxyHostPatterns ) );
            }

            configureAuthentication( httpClient, remoteProxySettings.getProxyAuthentication(), proxy );
        }
    }
}
