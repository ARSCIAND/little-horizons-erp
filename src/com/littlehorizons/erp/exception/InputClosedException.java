package com.littlehorizons.erp.exception;

/**
 * Signals that standard input was closed (e.g. Ctrl+D / end of a piped script),
 * so the application can shut down gracefully instead of looping forever.
 */
public class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("Standard input was closed");
    }
}
