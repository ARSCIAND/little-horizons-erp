package com.littlehorizons.erp.model.enums;

/** Status of a student's registration in a subject for a period. */
public enum SubjectEnrollmentStatus implements DbEnum {
    ACTIVE("Activa"),
    WITHDRAWN("Retirada"),
    COMPLETED("Completada");

    private final String dbValue;

    SubjectEnrollmentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
