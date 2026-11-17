package com.littlehorizons.erp.exception;

/**
 * Unchecked exception thrown by the DAO layer when a database operation fails.
 * The original {@link java.sql.SQLException} is kept as the cause and written to the error log.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
