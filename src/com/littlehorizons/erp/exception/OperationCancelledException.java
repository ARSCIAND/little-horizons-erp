package com.littlehorizons.erp.exception;

/** Thrown when the user types 0 inside a form or picker to go back without saving. */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operación cancelada por el usuario");
    }
}
