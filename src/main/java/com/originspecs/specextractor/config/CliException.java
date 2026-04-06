package com.originspecs.specextractor.config;

/**
 * Thrown when CLI argument parsing or validation fails.
 * Caught in {@code Main} and translated to a {@code System.exit(1)}.
 */
public final class CliException extends RuntimeException {

    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Throwable cause) {
        super(message, cause);
    }
}
