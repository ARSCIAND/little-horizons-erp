package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared formatting helpers for currency, grades and dates (Colombian conventions). */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    private static final DecimalFormat CURRENCY;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY = new DecimalFormat("$ #,##0", symbols);
    }

    private Formats() {
    }

    /** Formats a peso amount, e.g. {@code $ 350.000}. */
    public static synchronized String money(BigDecimal value) {
        return value == null ? "-" : CURRENCY.format(value.setScale(0, RoundingMode.HALF_UP));
    }

    public static String money(double value) {
        return money(BigDecimal.valueOf(value));
    }

    /** Formats a grade on the 0.0 - 5.0 scale with one decimal. */
    public static String grade(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public static String grade(BigDecimal value) {
        return value == null ? "-" : value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** Grades are reported with one decimal, so classifications use the rounded value. */
    public static double roundToTenth(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    public static String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Colombian performance scale (Decreto 1290 de 2009) used by most schools on a 1.0-5.0 range. */
    public static String performanceLevel(double rawGrade) {
        double grade = roundToTenth(rawGrade);
        if (grade >= 4.6) return "Superior";
        if (grade >= 4.0) return "Alto";
        if (grade >= 3.0) return "Básico";
        return "Bajo";
    }
}
