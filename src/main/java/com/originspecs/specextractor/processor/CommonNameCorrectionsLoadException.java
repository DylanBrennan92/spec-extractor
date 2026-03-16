package com.originspecs.specextractor.processor;

/**
 * Thrown when the common-name corrections properties file cannot be loaded.
 * Callers may choose to fail the pipeline or continue without corrections.
 */
public class CommonNameCorrectionsLoadException extends RuntimeException {

    public CommonNameCorrectionsLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
