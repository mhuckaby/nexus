package org.sonatype.nexus.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Thread which listens for command messages to control the JVM.
 *
 * @since 2.1
 */
public class CommandMonitorThread
    extends Thread
{
    private static final Logger log = LoggerFactory.getLogger(CommandMonitorThread.class);

    private static final String STOP_COMMAND = "STOP";

    private static final String RESTART_COMMAND = "RESTART";

    private final ServerSocket socket;

    public CommandMonitorThread(final int port) throws IOException {
        setDaemon(true);
        setName("Bootstrap Command Monitor");
        // Only listen on local interface
        this.socket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }

    @Override
    public void run() {
        log.info("Listening for commands: {}", socket);

        boolean running = true;
        while (running) {
            try {
                Socket client = socket.accept();
                log.info("Accepted client: {}", client);

                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String command = reader.readLine();
                log.info("Read command: {}", command);
                client.close();

                if (STOP_COMMAND.equals(command)) {
                    log.info("Requesting application stop");
                    WrapperManager.stopAndReturn(0);
                    running = false;
                }
                else if (RESTART_COMMAND.equals(command)) {
                    log.info("Requesting applciation restart");
                    WrapperManager.restartAndReturn();
                }
                else {
                    log.error("Unknown command: {}", command);
                }
            }
            catch (Exception e) {
                log.error("Failed", e);
            }
        }

        try {
            socket.close();
        }
        catch (IOException e) {
            // ignore
        }

        log.info("Stopped");
    }
}
