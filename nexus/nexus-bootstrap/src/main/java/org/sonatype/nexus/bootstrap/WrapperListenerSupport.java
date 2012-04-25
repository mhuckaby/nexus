package org.sonatype.nexus.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperListener;

import java.util.Arrays;

/**
 * Support for {@link WrapperListener} implementations.
 *
 * @since 2.1
 */
public abstract class WrapperListenerSupport
    implements WrapperListener
{
    protected final Logger log = LoggerFactory.getLogger(getClass());

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

    protected abstract Integer doStart(final String[] args) throws Exception;

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

    protected abstract int doStop(final int code) throws Exception;

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

    protected abstract void doControlEvent(final int code) throws Exception;
}
