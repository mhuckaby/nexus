package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.sonatype.appcontext.AppContext;
import org.sonatype.appcontext.AppContextEntry;
import org.sonatype.appcontext.AppContextRequest;
import org.sonatype.appcontext.Factory;
import org.sonatype.appcontext.publisher.EntryPublisher;
import org.sonatype.appcontext.source.PropertiesFileEntrySource;
import org.sonatype.appcontext.source.StaticEntrySource;
import org.sonatype.sisu.jetty.Jetty8;
import org.tanukisoftware.wrapper.WrapperManager;

import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT;

/**
 * Nexus bootstrap launcher.
 * 
 * @since 2.1
 */
public class Launcher
    extends WrapperListenerSupport
{
    private static final String BUNDLEBASEDIR_KEY = "bundleBasedir";
    
    private Jetty8 server;

    @Override
    protected Integer doStart( final String[] args )
        throws Exception
    {
        if ( WrapperManager.isControlledByNativeWrapper() )
        {
            log.info( "JVM ID: {}, JVM PID: {}, Wrapper PID: {}, User: {}",
                new Object[] { WrapperManager.getJVMId(), WrapperManager.getJavaPID(), WrapperManager.getWrapperPID(),
                    WrapperManager.getUser( false ).getUser() } );
        }

        File cwd = new File( "." ).getCanonicalFile();
        log.info( "Current directory: {}", cwd );

        if ( args.length != 1 )
        {
            log.error( "Missing Jetty configuration file parameter" );
            return 1; // exit
        }
        
        // we have three properties file:
        // jvm.properties -- mandatory, will be picked up into context and published to JVM System Properties
        // this is the place to set java.io.tmp and debug options by users
        
        // nexus.properties -- mandatory, will be picked up into context and NOT published to JVM System Properties
        // this is place to set nexus properties like workdir location etc (as today)
        
        // nexus-test.properties -- optional, if present, will override values from those above
        // this is place to set test properties (like jetty port) etc 

        // create app context request, with ID "nexus", without parent, and due to NEXUS-4520 add "plexus" alias too
        final AppContextRequest request = Factory.getDefaultRequest( "nexus", null, Arrays.asList( "plexus" ) );

        // note: sources list is "ascending by importance", 1st elem in list is "weakest" and last elem in list is
        // "strongest" (overrides). Factory already created us some sources, so we are just adding to that list without
        // disturbing the order of the list (we add to list head and tail)
        
        // add the jvm.properties, is mandatory to be present (and we need ref to this source, see below)
        final PropertiesFileEntrySource jvmProperties =
            new PropertiesFileEntrySource( new File( cwd, "jvm.properties" ), true );
        request.getSources().add( 0, jvmProperties );
        // add the nexus.properties, is mandatory to be present
        request.getSources().add( 1, new PropertiesFileEntrySource( new File( cwd, "nexus.properties" ), true ) );
        // add the nexus-test.properties, not mandatory to be present
        request.getSources().add( 2, new PropertiesFileEntrySource( new File( cwd, "nexus-test.properties" ), false ) );

        // ultimate source of "bundleBasedir" (hence, is added as last in sources list)
        // TODO: what happens if user added "bundleBasedir" to jvm.properties file?
        // Now, that will be always overriden by value got from cwd and that seems correct to me
        request.getSources().add( new StaticEntrySource( BUNDLEBASEDIR_KEY, cwd.getAbsolutePath() ) );

        // by default, publishers list will contain one "dump" publisher and hence, on creation, a dump will be written
        // out (to System.out or SLF4J logger, depending is latter on classpath or not)
        // if we dont want to "mute" this dump, just uncomment this below
        // request.getPublishers().clear();

        // publishers (order does not matter for us, unlike sources)
        // we need to publish one property: "bundleBasedir"
        request.getPublishers().add( new EntryPublisher()
        {
            @Override
            public void publishEntries( final AppContext context )
            {
                System.setProperty( BUNDLEBASEDIR_KEY, String.valueOf( context.get( BUNDLEBASEDIR_KEY ) ) );
            }
        } );

        // we need to publish all entries coming from jvm.properties
        request.getPublishers().add( new EntryPublisher()
        {
            @Override
            public void publishEntries( final AppContext context )
            {
                for ( String key : context.keySet() )
                {
                    final AppContextEntry entry = context.getAppContextEntry( key );
                    if ( entry.getEntrySourceMarker() == jvmProperties )
                    {
                        System.setProperty( key, String.valueOf( entry.getValue() ) );
                    }
                }
            }
        } );

        // create the context and use it as "parent" for Jetty8
        // when context created, the context is built and all publisher were invoked (system props set for example)
        final AppContext context = Factory.create( request );

        server = new Jetty8( new File( args[0] ), context );

        ensureTmpDirSanity();
        maybeEnableCommandMonitor();

        server.startJetty();
        return null; // continue running
    }

    protected void ensureTmpDirSanity()
        throws IOException
    {
        // Make sure that java.io.tmpdir points to a real directory
        String tmp = System.getProperty( "java.io.tmpdir", "tmp" );
        File file = new File( tmp );
        if ( !file.exists() )
        {
            if ( file.mkdirs() )
            {
                log.info( "Created tmp dir: {}", file );
            }
        }
        else if ( !file.isDirectory() )
        {
            log.warn( "Tmp dir is configured to a location which is not a directory: {}", file );
        }

        // Ensure we can actually create a new tmp file
        file = File.createTempFile( getClass().getSimpleName(), ".tmp" );
        file.createNewFile();
        File tmpDir = file.getCanonicalFile().getParentFile();
        log.info( "Temp directory: {}", tmpDir );
        System.setProperty( "java.io.tmpdir", tmpDir.getAbsolutePath() );
        file.delete();
    }

    protected void maybeEnableCommandMonitor()
        throws IOException
    {
        String commandMonitorPort = System.getProperty( CommandMonitorThread.class.getName() + ".port" );
        if ( commandMonitorPort != null )
        {
            new CommandMonitorThread( Integer.parseInt( commandMonitorPort ) ).start();
        }
    }

    @Override
    protected int doStop( final int code )
        throws Exception
    {
        server.stopJetty();
        return code;
    }

    @Override
    protected void doControlEvent( final int code )
    {
        if ( WRAPPER_CTRL_LOGOFF_EVENT == code && WrapperManager.isLaunchedAsService() )
        {
            log.debug( "Launched as a service; ignoring event: {}", code );
        }
        else
        {
            log.debug( "Stopping" );
            WrapperManager.stop( 0 );
            throw new Error( "unreachable" );
        }
    }

    /**
     * Bridges standard Java entry-point into JSW.
     */
    public static void main( final String[] args )
        throws Exception
    {
        WrapperManager.start( new Launcher(), args );
    }
}
