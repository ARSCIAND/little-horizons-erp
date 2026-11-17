package com.littlehorizons.erp.model.enums;

/** Position obtained in competitive events. */
public enum CompetitionPosition implements DbEnum {
    FIRST_PLACE("1er Lugar"),
    SECOND_PLACE("2do Lugar"),
    THIRD_PLACE("3er Lugar"),
    PARTICIPANT("Participante");

    private final String dbValue;

    CompetitionPosition(String dbValue) {
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
