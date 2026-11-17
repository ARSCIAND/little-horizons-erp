package com.littlehorizons.erp.model.enums;

/** Academic period a payment applies to. */
public enum PaymentPeriod implements DbEnum {
    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    ANNUAL("Anual");

    private final String dbValue;

    PaymentPeriod(String dbValue) {
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
