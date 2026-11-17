package com.littlehorizons.erp.model.enums;

/** Identity document types accepted by the school. */
public enum DocumentType implements DbEnum {
    IDENTITY_CARD("TI"),
    CITIZENSHIP_CARD("CC"),
    FOREIGNER_ID("CE"),
    PASSPORT("Pasaporte");

    private final String dbValue;

    DocumentType(String dbValue) {
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
