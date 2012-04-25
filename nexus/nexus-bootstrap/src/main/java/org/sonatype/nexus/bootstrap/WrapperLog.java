package org.sonatype.nexus.bootstrap;

import org.tanukisoftware.wrapper.WrapperManager;

import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_ADVICE;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_DEBUG;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_ERROR;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_FATAL;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_INFO;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_STATUS;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_LOG_LEVEL_WARN;

/**
 * Helper to emit messages via the JSW wrapper log stream.
 *
 * @since 2.1
 */
public class WrapperLog
{
    public static void log(final int level, final String message) {
        WrapperManager.log(level, message);
    }

    public static void debug(final String message) {
        log(WRAPPER_LOG_LEVEL_DEBUG, message);
    }

    public static void info(final String message) {
        log(WRAPPER_LOG_LEVEL_INFO, message);
    }

    public static void status(final String message) {
        log(WRAPPER_LOG_LEVEL_STATUS, message);
    }

    public static void warn(final String message) {
        log(WRAPPER_LOG_LEVEL_WARN, message);
    }

    public static void error(final String message) {
        log(WRAPPER_LOG_LEVEL_ERROR, message);
    }

    public static void fatal(final String message) {
        log(WRAPPER_LOG_LEVEL_FATAL, message);
    }

    public static void fatal(final String message, final Throwable cause) {
        log(WRAPPER_LOG_LEVEL_FATAL, message);
        cause.printStackTrace();
    }

    public static void advice(final String message) {
        log(WRAPPER_LOG_LEVEL_ADVICE, message);
    }
}
