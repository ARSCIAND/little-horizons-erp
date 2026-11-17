package com.littlehorizons.erp.model.enums;

/** Status of a student's membership in a club. */
public enum MembershipStatus implements DbEnum {
    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String dbValue;

    MembershipStatus(String dbValue) {
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
