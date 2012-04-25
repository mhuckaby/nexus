package org.sonatype.nexus.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Talks to the command monitor.
 *
 * @since 2.1
 */
public class CommandMonitorTalker
{
    private static Logger log = LoggerFactory.getLogger(CommandMonitorTalker.class);

    private final String host;

    private final int port;

    public CommandMonitorTalker(final String host, final int port) {
        if (host == null) {
            throw new NullPointerException();
        }
        this.host = host;
        if (port < 1) {
            throw new IllegalArgumentException("Invalid port");
        }
        this.port = port;
    }

    public void send(final String command) {
        if (command == null) {
            throw new NullPointerException();
        }

        log.debug("Sending command: {}", command);

        try {
            Socket socket = new Socket();
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress(host, port));
            try {
                OutputStream output = socket.getOutputStream();
                output.write(command.getBytes());
                output.close();
            }
            finally {
                socket.close();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
