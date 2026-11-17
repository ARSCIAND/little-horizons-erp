package com.littlehorizons.erp.model.enums;

/** Availability status of a lendable resource. */
public enum ResourceStatus implements DbEnum {
    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    DECOMMISSIONED("Dado de Baja");

    private final String dbValue;

    ResourceStatus(String dbValue) {
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
