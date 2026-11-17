package com.littlehorizons.erp.exception;

/**
 * Thrown by controllers when a business rule is violated
 * (e.g. club capacity reached, resource not available, duplicated document).
 * Its message is always safe to show to the end user.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
