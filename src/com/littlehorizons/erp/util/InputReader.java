package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.enums.DbEnum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validated console input. Every method keeps asking until the user types a valid value,
 * showing a clear error message on each failed attempt (requirement 5.2).
 */
public final class InputReader {

    public static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    public static final Pattern PHONE = Pattern.compile("^[0-9+() -]{7,20}$");
    public static final Pattern DOCUMENT = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
    private static final Pattern GRADE = Pattern.compile("^[0-5]([.,][0-9])?$");

    private final BufferedReader reader;

    public InputReader() {
        String encoding = System.getProperty("stdin.encoding");
        Charset charset = encoding != null && Charset.isSupported(encoding) ? Charset.forName(encoding) : Charset.defaultCharset();
        this.reader = new BufferedReader(new InputStreamReader(System.in, charset));
    }

    /** Typing this key (and Enter) in any form cancels it and goes back to the menu. */
    public static final String CANCEL_KEY = "x";

    /**
     * Reads a line. If the user types {@value #CANCEL_KEY} the current operation is cancelled
     * ({@link OperationCancelledException}), so every form can be left at any moment.
     */
    public String readLine(String prompt) {
        String line = readRaw(prompt);
        if (line.equalsIgnoreCase(CANCEL_KEY)) {
            throw new OperationCancelledException();
        }
        return line;
    }

    /** Reads a raw line; throws {@link InputClosedException} if stdin reached its end. */
    private String readRaw(String prompt) {
        System.out.print(prompt);
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new InputClosedException();
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                error("Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                error("'" + text + "' no es un número entero válido.");
            }
        }
    }

    /** Returns empty when the user just presses Enter. */
    public Optional<Integer> readOptionalInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return Optional.empty();
            }
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return Optional.of(value);
                }
                error("Ingrese un número entre " + min + " y " + max + " (o Enter para omitir).");
            } catch (NumberFormatException e) {
                error("'" + text + "' no es un número entero válido.");
            }
        }
    }

    public String readText(String prompt, int minLength, int maxLength) {
        while (true) {
            String text = readLine(prompt);
            if (text.length() < minLength) {
                error(minLength == 1 ? "Este campo es obligatorio." : "Debe tener al menos " + minLength + " caracteres.");
            } else if (text.length() > maxLength) {
                error("Máximo " + maxLength + " caracteres.");
            } else {
                return text;
            }
        }
    }

    /** Optional free text: returns {@code null} when left blank. */
    public String readOptionalText(String prompt, int maxLength) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            if (text.length() <= maxLength) {
                return text;
            }
            error("Máximo " + maxLength + " caracteres.");
        }
    }

    /** Required person name: letters, spaces, apostrophes and hyphens only. */
    public String readName(String prompt) {
        return readMatching(prompt, Pattern.compile("^[\\p{L}][\\p{L} '\\-]{0,99}$"), "Solo se permiten letras y espacios.", false);
    }

    public String readOptionalName(String prompt) {
        return readMatching(prompt, Pattern.compile("^[\\p{L}][\\p{L} '\\-]{0,99}$"), "Solo se permiten letras y espacios.", true);
    }

    public String readDocument(String prompt) {
        return readMatching(prompt, DOCUMENT, "Documento inválido: use 5 a 20 dígitos/letras sin espacios.", false);
    }

    public String readPhone(String prompt, boolean optional) {
        return readMatching(prompt, PHONE, "Teléfono inválido: use entre 7 y 20 dígitos.", optional);
    }

    public String readEmail(String prompt, boolean optional) {
        String value = readMatching(prompt, EMAIL, "Correo electrónico inválido (ej: nombre@dominio.com).", optional);
        return value == null ? null : value.toLowerCase();
    }

    public String readMatching(String prompt, Pattern pattern, String errorMessage, boolean optional) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && optional) {
                return null;
            }
            if (text.isEmpty()) {
                error("Este campo es obligatorio.");
            } else if (!pattern.matcher(text).matches()) {
                error(errorMessage);
            } else {
                return text;
            }
        }
    }

    /** Reads a date in yyyy-MM-dd format that satisfies the given rule. */
    public LocalDate readDate(String prompt, Predicate<LocalDate> rule, String ruleMessage) {
        while (true) {
            String text = readLine(prompt);
            try {
                LocalDate date = LocalDate.parse(text, Formats.DATE);
                if (rule.test(date)) {
                    return date;
                }
                error(ruleMessage);
            } catch (DateTimeParseException e) {
                error("Fecha inválida. Use el formato AAAA-MM-DD (ej: 2026-03-15).");
            }
        }
    }

    /** Date that defaults to {@code defaultValue} when the user presses Enter. */
    public LocalDate readDateOrDefault(String prompt, LocalDate defaultValue, Predicate<LocalDate> rule, String ruleMessage) {
        while (true) {
            String text = readLine(prompt + " [Enter = " + Formats.date(defaultValue) + "]: ");
            if (text.isEmpty()) {
                return defaultValue;
            }
            try {
                LocalDate date = LocalDate.parse(text, Formats.DATE);
                if (rule.test(date)) {
                    return date;
                }
                error(ruleMessage);
            } catch (DateTimeParseException e) {
                error("Fecha inválida. Use el formato AAAA-MM-DD.");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return LocalTime.parse(text.length() == 4 ? "0" + text : text);
            } catch (DateTimeParseException e) {
                error("Hora inválida. Use formato 24 horas HH:MM (ej: 07:00, 14:30).");
            }
        }
    }

    public BigDecimal readMoney(String prompt, BigDecimal min) {
        while (true) {
            String text = readLine(prompt).replace("$", "").replace(".", "").replace(",", ".").replace(" ", "");
            try {
                BigDecimal value = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
                if (value.compareTo(min) >= 0 && value.compareTo(new BigDecimal("99999999.99")) <= 0) {
                    return value;
                }
                error("El valor debe estar entre " + Formats.money(min) + " y $ 99.999.999.");
            } catch (NumberFormatException e) {
                error("Valor inválido. Escriba solo números, ej: 350000");
            }
        }
    }

    /** Grade on the Colombian scale 0.0 - 5.0 with exactly one decimal (accepts comma or dot). */
    public BigDecimal readGrade(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!GRADE.matcher(text).matches()) {
                error("Nota inválida. Use un valor entre 0.0 y 5.0 con máximo un decimal (ej: 4.5).");
                continue;
            }
            BigDecimal value = new BigDecimal(text.replace(',', '.')).setScale(1, RoundingMode.UNNECESSARY);
            if (value.compareTo(new BigDecimal("5.0")) > 0) {
                error("La nota máxima es 5.0.");
                continue;
            }
            return value;
        }
    }

    /** Like {@link #readGrade(String)} but returns {@code null} when the user presses Enter. */
    public BigDecimal readOptionalGrade(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            if (GRADE.matcher(text).matches()) {
                BigDecimal value = new BigDecimal(text.replace(',', '.')).setScale(1, RoundingMode.UNNECESSARY);
                if (value.compareTo(new BigDecimal("5.0")) <= 0) {
                    return value;
                }
            }
            error("Nota inválida. Use un valor entre 0.0 y 5.0 (ej: 3.8) o Enter para omitir.");
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String text = readLine(prompt + " (s/n): ").toLowerCase();
            if (text.equals("s") || text.equals("si") || text.equals("sí")) return true;
            if (text.equals("n") || text.equals("no")) return false;
            error("Responda 's' para sí o 'n' para no.");
        }
    }

    /** Shows the enum options numbered from 1 and returns the chosen constant. */
    public <E extends Enum<E> & DbEnum> E readEnum(String title, E[] options) {
        System.out.println("  " + title + ":");
        for (int i = 0; i < options.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, options[i].getDbValue());
        }
        return options[readInt("  Seleccione una opción: ", 1, options.length) - 1];
    }

    public void pause() {
        readRaw("\n  Presione Enter para continuar...");
    }

    private static void error(String message) {
        System.out.println("  ✖ " + message + " Intente de nuevo.");
    }
}
