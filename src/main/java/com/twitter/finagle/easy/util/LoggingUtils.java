package com.twitter.finagle.easy.util;

import org.apache.commons.logging.Log;

/**
 * Wraps the Commons Logging API with varags
 *
 * @author ed.peters
 */
public final class LoggingUtils {

    private LoggingUtils() { }

    public static void debug(Log log, Throwable throwable,
                             String message, Object... args) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot format null message");
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format(message, args), throwable);
        }
    }

    public static void debug(Log log, String message, Object... args) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot format null message");
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format(message, args));
        }
    }

    public static void info(Log log, Throwable throwable,
                            String message, Object... args) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot format null message");
        }
        if(log.isInfoEnabled()) {
            log.info(String.format(message, args), throwable);
        }
    }

    public static void info(Log log, String message, Object... args) {
        if (message == null) {
            throw new IllegalArgumentException("Cannot format null message");
        }
        if(log.isInfoEnabled()) {
            log.info(String.format(message, args));
        }
    }

}
