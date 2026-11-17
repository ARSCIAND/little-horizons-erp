package com.littlehorizons.erp.model.enums;

/** Category of a school event. */
public enum EventType implements DbEnum {
    ACADEMIC("Académico"),
    CULTURAL("Cultural"),
    SPORTS("Deportivo"),
    SOCIAL("Social"),
    SCIENTIFIC("Científico");

    private final String dbValue;

    EventType(String dbValue) {
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
