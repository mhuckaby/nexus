package org.sonatype.nexus.bootstrap;

import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.jetty.Jetty8;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT;

/**
 * Nexus bootstrap launcher.
 *
 * @since 2.1
 */
public class Launcher
    implements WrapperListener
{
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private Jetty8 server;

    @Override
    public Integer start(final String[] args) {
        log.info("Starting with arguments: {}", Arrays.asList(args));

        try {
            return doStart(args);
        }
        catch (Exception e) {
            log.error("Failed to start", e);
            return 1; // exit
        }
    }

    protected Integer doStart(final String[] args) throws Exception {
        if (WrapperManager.isControlledByNativeWrapper()) {
            log.info("JVM ID: {}, JVM PID: {}, Wrapper PID: {}, User: {}", new Object[]{
                WrapperManager.getJVMId(),
                WrapperManager.getJavaPID(),
                WrapperManager.getWrapperPID(),
                WrapperManager.getUser(false).getUser()
            });
        }

        if (args.length != 1) {
            log.error("Missing Jetty configuration file parameter");
            return 1; // exit
        }
        File file = new File(args[0]);
        server = new Jetty8(file);

        ensureTmpDirSanity();
        maybeEnableCommandMonitor();

        server.startJetty();
        return null; // continue running
    }

    protected void ensureTmpDirSanity() throws IOException {
        // Make sure that java.io.tmpdir points to a real directory
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp != null) {
            File file = new File(tmp);
            if (!file.exists()) {
                if (file.mkdirs()) {
                    log.info("Created tmp dir: {}", file);
                }
            }
            else if (!file.isDirectory()) {
                log.warn("Tmp dir is configured to a location which is not a directory: {}", file);
            }

            // Ensure we can actually create a new tmp file
            file = File.createTempFile(getClass().getSimpleName(), ".tmp");
            file.createNewFile();
            log.debug("Tmp dir: {}", file.getCanonicalFile().getParent());
            file.delete();
        }
    }

    protected void maybeEnableCommandMonitor() throws IOException {
        String commandMonitorPort = System.getProperty(CommandMonitorThread.class.getName() + ".port");
        if (commandMonitorPort != null) {
            new CommandMonitorThread(Integer.parseInt(commandMonitorPort)).start();
        }
    }

    private void loadProperties(final Resource resource) throws IOException {
        assert resource != null;
        log.debug("Loading properties from: {}", resource);
        Properties props = new Properties();
        InputStream input = resource.getInputStream();
        try {
            props.load(input);
            if (log.isDebugEnabled()) {
                for (Map.Entry entry : props.entrySet()) {
                    log.debug("  {}='{}'", entry.getKey(), entry.getValue());
                }
            }
        }
        finally {
            input.close();
        }
        System.getProperties().putAll(props);
    }

    private void loadProperties(final String resource, final boolean required) throws IOException {
        URL url = getClass().getResource(resource);
        if (url == null) {
            if (required) {
                log.error("Missing resource: {}", resource);
            }
            else {
                log.debug("Missing optional resource: {}", resource);
            }
        }
        else {
            loadProperties(Resource.newResource(url));
        }
    }

    private void maybeSetProperty(final String name, final String value) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, value);
        }
    }

    @Override
    public int stop(final int code) {
        log.info("Stopping with code: {}", code);

        try {
            return doStop(code);
        }
        catch (Exception e) {
            log.error("Failed to stop cleanly", e);
            return 1; // exit
        }
    }


    protected int doStop(final int code) throws Exception {
        server.stopJetty();
        return code;
    }

    @Override
    public void controlEvent(final int code) {
        log.info("Received control event: {}", code);

        try {
            doControlEvent(code);
        }
        catch (Exception e) {
            log.error("Failed to handle control event[{}]", code, e);
        }
    }

    protected void doControlEvent(final int code) {
        if (WRAPPER_CTRL_LOGOFF_EVENT == code && WrapperManager.isLaunchedAsService()) {
            log.debug("Launched as a service; ignoring event: {}", code);
        }
        else {
            log.debug("Stopping");
            WrapperManager.stop(0);
            throw new Error("unreachable");
        }
    }

    /**
     * Bridges standard Java entry-point into JSW.
     */
    public static void main(final String[] args) throws Exception {
        WrapperManager.start(new Launcher(), args);
    }
}
