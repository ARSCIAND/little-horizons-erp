package com.littlehorizons.erp.model.enums;

/** Academic periods of the Colombian school year plus the final (definitive) grade. */
public enum AcademicPeriod implements DbEnum {
    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    FINAL("Final");

    private final String dbValue;

    AcademicPeriod(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    /** The four regular periods (excludes the final grade). */
    public static AcademicPeriod[] regularPeriods() {
        return new AcademicPeriod[]{FIRST, SECOND, THIRD, FOURTH};
    }

    public boolean isRegular() {
        return this != FINAL;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
