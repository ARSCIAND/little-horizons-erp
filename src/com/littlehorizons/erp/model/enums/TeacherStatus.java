package com.littlehorizons.erp.model.enums;

/** Employment status of a teacher. */
public enum TeacherStatus implements DbEnum {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String dbValue;

    TeacherStatus(String dbValue) {
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
