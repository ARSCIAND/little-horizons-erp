package com.littlehorizons.erp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Renders lists of objects as aligned text tables in the console.
 * <p>
 * Columns are declared with a header and a lambda that extracts the cell text, which keeps
 * views short: {@code TablePrinter.of(students).column("ID", s -> ...).print();}
 *
 * @param <T> type of the rows
 */
public final class TablePrinter<T> {

    private static final int MAX_CELL_WIDTH = 42;

    private final List<T> rows;
    private final List<String> headers = new ArrayList<>();
    private final List<Function<T, Object>> extractors = new ArrayList<>();

    private TablePrinter(List<T> rows) {
        this.rows = rows;
    }

    public static <T> TablePrinter<T> of(List<T> rows) {
        return new TablePrinter<>(rows);
    }

    public TablePrinter<T> column(String header, Function<T, Object> extractor) {
        headers.add(header);
        extractors.add(extractor);
        return this;
    }

    public void print() {
        if (rows.isEmpty()) {
            System.out.println("  (No hay registros para mostrar)");
            return;
        }
        List<String[]> cells = rows.stream()
                .map(row -> extractors.stream()
                        .map(extractor -> clip(String.valueOf(valueOrDash(extractor.apply(row)))))
                        .toArray(String[]::new))
                .toList();

        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            final int column = i;
            widths[i] = Math.max(headers.get(i).length(),
                    cells.stream().mapToInt(c -> c[column].length()).max().orElse(0));
        }

        String border = buildBorder(widths);
        System.out.println(border);
        System.out.println(buildRow(headers.toArray(String[]::new), widths));
        System.out.println(border.replace('-', '='));
        cells.forEach(c -> System.out.println(buildRow(c, widths)));
        System.out.println(border);
        System.out.printf("  Total: %d registro(s)%n", rows.size());
    }

    private static Object valueOrDash(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value;
    }

    private static String clip(String text) {
        return text.length() <= MAX_CELL_WIDTH ? text : text.substring(0, MAX_CELL_WIDTH - 1) + "…";
    }

    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder("  +");
        Arrays.stream(widths).forEach(w -> sb.append("-".repeat(w + 2)).append('+'));
        return sb.toString();
    }

    private static String buildRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < values.length; i++) {
            sb.append(' ').append(String.format("%-" + widths[i] + "s", values[i])).append(" |");
        }
        return sb.toString();
    }
}
