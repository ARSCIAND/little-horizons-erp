package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts peso amounts to Spanish words for invoices, e.g. 350000 → "TRESCIENTOS CINCUENTA MIL PESOS M/CTE". */
public final class NumberToWords {

    private static final String[] UNITS = {"", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
            "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis", "veintisiete",
            "veintiocho", "veintinueve"};
    private static final String[] TENS = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"};
    private static final String[] HUNDREDS = {"", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
            "seiscientos", "setecientos", "ochocientos", "novecientos"};

    private NumberToWords() {
    }

    public static String pesos(BigDecimal amount) {
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        String words = value == 0 ? "cero" : convert(value).trim();
        String suffix = (value % 1_000_000 == 0 && value > 0) ? " de pesos m/cte" : " pesos m/cte";
        return (words + suffix).toUpperCase();
    }

    private static String convert(long n) {
        if (n >= 1_000_000) {
            long millions = n / 1_000_000;
            String head = millions == 1 ? "un millón" : apocope(convert(millions)) + " millones";
            return head + (n % 1_000_000 == 0 ? "" : " " + convert(n % 1_000_000));
        }
        if (n >= 1000) {
            long thousands = n / 1000;
            String head = thousands == 1 ? "mil" : apocope(convert(thousands)) + " mil";
            return head + (n % 1000 == 0 ? "" : " " + convert(n % 1000));
        }
        if (n >= 100) {
            if (n == 100) return "cien";
            return HUNDREDS[(int) (n / 100)] + (n % 100 == 0 ? "" : " " + convert(n % 100));
        }
        if (n < 30) {
            return UNITS[(int) n];
        }
        return TENS[(int) (n / 10)] + (n % 10 == 0 ? "" : " y " + UNITS[(int) (n % 10)]);
    }

    /** "uno" becomes "un" / "veintiuno" becomes "veintiún" before "mil" and "millones". */
    private static String apocope(String words) {
        if (words.endsWith("veintiuno")) return words.substring(0, words.length() - 9) + "veintiún";
        if (words.endsWith("uno")) return words.substring(0, words.length() - 3) + "un";
        return words;
    }
}
