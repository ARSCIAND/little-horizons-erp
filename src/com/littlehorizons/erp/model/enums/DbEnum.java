package com.littlehorizons.erp.model.enums;

import java.util.Arrays;

/**
 * Contract for enums whose constants are stored in MySQL ENUM columns.
 * Java constants stay in English while {@link #getDbValue()} returns the exact Spanish value
 * defined in the database script.
 */
public interface DbEnum {

    String getDbValue();

    /** Resolves the constant whose database value matches {@code value} (case-insensitive). */
    static <E extends Enum<E> & DbEnum> E fromDb(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.getDbValue().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + value));
    }
}
