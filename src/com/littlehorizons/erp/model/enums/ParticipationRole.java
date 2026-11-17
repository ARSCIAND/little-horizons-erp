package com.littlehorizons.erp.model.enums;

/** Role a student plays in an event. */
public enum ParticipationRole implements DbEnum {
    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String dbValue;

    ParticipationRole(String dbValue) {
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
