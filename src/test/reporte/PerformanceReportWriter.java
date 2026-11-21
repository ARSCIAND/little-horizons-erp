package test.reporte;

import test.modelo.PerformanceReport;
import test.modelo.StudentPerformance;
import test.modelo.SubjectSummary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Writes the academic performance report as a UTF-8 .txt file inside the reports/ folder. */
public class PerformanceReportWriter {

    public static final Path REPORTS_DIRECTORY = Paths.get("reports");

    private static final int WIDTH = 92;
    private static final String DOUBLE_LINE = "=".repeat(WIDTH);
    private static final String SINGLE_LINE = "-".repeat(WIDTH);
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Writes the report and returns the absolute path of the created file. */
    public Path write(PerformanceReport report) throws IOException {
        Files.createDirectories(REPORTS_DIRECTORY);
        String fileName = String.format("rendimiento_%s_%d_%s.txt", slug(report.grade().name()),
                report.academicYear(), report.generatedAt().format(FILE_STAMP));
        Path file = REPORTS_DIRECTORY.resolve(fileName);
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(render(report));
        }
        return file.toAbsolutePath();
    }

    /** Builds the whole text of the report (also used to preview it in the console). */
    public String render(PerformanceReport report) {
        StringBuilder sb = new StringBuilder();
        line(sb, DOUBLE_LINE);
        line(sb, center("COLEGIO INTERNACIONAL LITTLE HORIZONS"));
        line(sb, center("REPORTE DE RENDIMIENTO ACADÉMICO - GRADO " + report.grade().name().toUpperCase()
                + " (AÑO " + report.academicYear() + ")"));
        line(sb, DOUBLE_LINE);
        line(sb, "Fecha de generación : " + report.generatedAt().format(SHOWN));
        line(sb, "Criterio            : estudiantes con matrícula Activa en " + report.academicYear()
                + ", notas de los períodos I a IV");
        line(sb, "Escala de desempeño : Superior (4.6-5.0) | Alto (4.0-4.5) | Básico (3.0-3.9) | Bajo (0.0-2.9)");
        line(sb, "");

        line(sb, "1. RANKING DE ESTUDIANTES");
        line(sb, SINGLE_LINE);
        line(sb, String.format("%-6s %-38s %-15s %8s  %-9s %s", "Puesto", "Estudiante", "Documento", "Promedio",
                "Desempeño", "Observación"));
        line(sb, SINGLE_LINE);
        int position = 1;
        for (StudentPerformance p : report.ranking()) {
            line(sb, String.format(Locale.US, "%-6d %-38s %-15s %8.2f  %-9s %s", position++, clip(p.student().fullName(), 38),
                    p.student().document(), p.average(), p.level(), observation(p)));
        }
        line(sb, SINGLE_LINE);
        line(sb, "");

        line(sb, "2. PROMEDIO DEL CURSO POR MATERIA");
        line(sb, SINGLE_LINE);
        line(sb, String.format("%-30s %8s %8s %8s  %s", "Materia", "Promedio", "Máxima", "Mínima", "Mejor estudiante"));
        line(sb, SINGLE_LINE);
        for (SubjectSummary s : report.subjects()) {
            line(sb, String.format(Locale.US, "%-30s %8.2f %8.2f %8.2f  %s", clip(s.subject(), 30), s.average(), s.best(),
                    s.worst(), s.bestName()));
        }
        line(sb, SINGLE_LINE);
        line(sb, "");

        line(sb, "3. DETALLE POR ESTUDIANTE (promedio de cada materia)");
        line(sb, SINGLE_LINE);
        for (StudentPerformance p : report.ranking()) {
            line(sb, String.format(Locale.US, "%s (%s) - promedio general %.2f", p.student().fullName(),
                    p.student().document(), p.average()));
            p.subjectAverages().forEach((subject, average) ->
                    line(sb, String.format(Locale.US, "    %-30s %.2f%s", subject, average, average < 3.0 ? "  <- por debajo de 3.0" : "")));
        }
        line(sb, SINGLE_LINE);
        line(sb, "");

        line(sb, "4. RESUMEN");
        line(sb, SINGLE_LINE);
        line(sb, "Estudiantes evaluados        : " + report.ranking().size());
        line(sb, String.format(Locale.US, "Promedio general del curso   : %.2f", report.courseAverage()));
        line(sb, "Estudiantes por desempeño    : " + report.levelCounts().entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(" | ")));
        line(sb, "Candidatos a beca (> 4.5)    : " + names(report.ranking().stream()
                .filter(StudentPerformance::scholarshipCandidate).collect(Collectors.toList())));
        line(sb, "Requieren refuerzo (< 3.0)   : " + names(report.ranking().stream()
                .filter(StudentPerformance::needsSupport).collect(Collectors.toList())));
        line(sb, DOUBLE_LINE);
        line(sb, center("Generado por Little Horizons ERP - Buscador avanzado (prototipo)"));
        line(sb, DOUBLE_LINE);
        return sb.toString();
    }

    private static String observation(StudentPerformance p) {
        if (p.scholarshipCandidate()) return "Candidato a beca";
        if (p.needsSupport()) return "Requiere refuerzo";
        long failing = p.subjectAverages().values().stream().filter(average -> average < 3.0).count();
        return failing > 0 ? failing + " materia(s) por debajo de 3.0" : "-";
    }

    private static String names(List<StudentPerformance> students) {
        return students.isEmpty() ? "ninguno" : students.stream()
                .map(p -> p.student().fullName())
                .collect(Collectors.joining(", "));
    }

    private static void line(StringBuilder sb, String text) {
        sb.append(text).append(System.lineSeparator());
    }

    private static String center(String text) {
        return " ".repeat(Math.max(0, (WIDTH - text.length()) / 2)) + text;
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /** "Décimo" -> "decimo", for a safe file name. */
    private static String slug(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
    }
}
