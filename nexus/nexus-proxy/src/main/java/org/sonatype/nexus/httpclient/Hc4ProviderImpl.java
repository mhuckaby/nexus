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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.apache.http.client.HttpClient;
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
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

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
    implements Hc4Provider
{
    /**
     * Application configuration holding the {@link GlobalRemoteConnectionSettings}.
     */
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Parameters source for HC4.
     */
    private final Hc4Parameters parameters;

    /**
     * Shared client connection manager.
     */
    private ClientConnectionManager sharedConnectionManager;

    /**
     * Constructor.
     * 
     * @param applicationConfiguration the Nexus {@link ApplicationConfiguration}.
     * @param parameters the {@link Hc4Parameters} to be used with this component.
     */
    @Inject
    public Hc4ProviderImpl( final ApplicationConfiguration applicationConfiguration, final Hc4Parameters parameters )
    {
        this.applicationConfiguration = Preconditions.checkNotNull( applicationConfiguration );
        this.parameters = Preconditions.checkNotNull( parameters );
        this.sharedConnectionManager = null;
    }

    protected synchronized void configure()
    {
        if ( sharedConnectionManager == null )
        {
            sharedConnectionManager =
                createConnectionManager( applicationConfiguration.getGlobalRemoteStorageContext() );
        }
    }

    protected synchronized void shutdown()
    {
        if ( sharedConnectionManager != null )
        {
            sharedConnectionManager.shutdown();
            sharedConnectionManager = null;
        }
    }

    // == Hc4Provider API

    @Override
    public synchronized HttpClient createHttpClient()
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
    public synchronized DefaultHttpClient createHttpClient( final RemoteStorageContext context )
    {
        if ( sharedConnectionManager == null )
        {
            configure();
        }
        return createHttpClient( context, new ManagedByOtherClientConnectionManager( sharedConnectionManager ) );
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

    protected ClientConnectionManager createClientConnectionManager( final RemoteStorageContext context )
    {
        return new EvictingClientConnectionManager( createConnectionManager( context ),
            parameters.getConnectionPoolKeepalive( context ) );
    }

    protected HttpParams createHttpParams( final RemoteStorageContext context )
    {
        HttpParams params = new SyncBasicHttpParams();
        params.setParameter( HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1 );
        params.setBooleanParameter( HttpProtocolParams.USE_EXPECT_CONTINUE, false );
        params.setBooleanParameter( HttpConnectionParams.STALE_CONNECTION_CHECK, false );
        params.setIntParameter( HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024 );
        params.setLongParameter( ClientPNames.CONN_MANAGER_TIMEOUT, parameters.getConnectionPoolTimeout( context ) );
        params.setIntParameter( HttpConnectionParams.CONNECTION_TIMEOUT, parameters.getConnectionTimeout( context ) );
        params.setIntParameter( HttpConnectionParams.SO_TIMEOUT, parameters.getSoTimeout( context ) );
        return params;
    }

    protected ClientConnectionManager createConnectionManager( final RemoteStorageContext context )
        throws IllegalStateException
    {
        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register( new Scheme( "http", 80, PlainSocketFactory.getSocketFactory() ) );
        schemeRegistry.register( new Scheme( "https", 443, SSLSocketFactory.getSocketFactory() ) );
        final PoolingClientConnectionManager connManager = new PoolingClientConnectionManager( schemeRegistry );

        final int maxConnectionCount = parameters.getConnectionPoolMaxSize( context );
        final int perRouteConnectionCount = Math.min( parameters.getConnectionPoolSize( context ), maxConnectionCount );

        connManager.setMaxTotal( maxConnectionCount );
        connManager.setDefaultMaxPerRoute( perRouteConnectionCount );
        return connManager;
    }

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
