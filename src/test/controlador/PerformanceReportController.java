package test.controlador;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.ErrorLogger;
import test.modelo.GradeOption;
import test.modelo.PerformanceReport;
import test.modelo.StudentPerformance;
import test.modelo.StudentSearchResult;
import test.modelo.SubjectSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds the academic performance report of a grade for 2025.
 * <p>
 * It reuses the search by grade (Stream API filter) to know which students belong to the
 * grade, reads their 2025 grades and computes, with streams: the average of each subject,
 * the general average of each student, the ranking, the performance level, the course
 * statistics per subject and the number of students per level.
 */
public class PerformanceReportController {

    /** Colombian performance scale (Decreto 1290 de 2009). */
    public static final List<String> LEVELS = List.of("Superior", "Alto", "Básico", "Bajo");

    private static final String SQL_GRADES_2025 = """
            SELECT r.id_estudiante, m.nombre_materia, r.valor_nota
              FROM registros_academicos r
              JOIN materias m ON m.id_materia = r.id_materia
             WHERE r.año_academico = ?
               AND r.periodo_academico IN ('I', 'II', 'III', 'IV')
            """;

    /** One grade (nota) read from the database. */
    private record GradeEntry(int studentId, String subject, double value) {
    }

    private final StudentSearchController searchController;
    private final DatabaseConnection database;

    public PerformanceReportController(StudentSearchController searchController, DatabaseConnection database) {
        this.searchController = searchController;
        this.database = database;
    }

    public PerformanceReport build(GradeOption grade) {
        List<StudentSearchResult> students = searchController.searchByGrade(grade);
        Set<Integer> studentIds = students.stream()
                .map(StudentSearchResult::studentId)
                .collect(Collectors.toSet());

        List<GradeEntry> entries = loadGrades().stream()
                .filter(entry -> studentIds.contains(entry.studentId()))
                .collect(Collectors.toList());

        List<StudentPerformance> ranking = students.stream()
                .map(student -> toPerformance(student, entries))
                .filter(performance -> !performance.subjectAverages().isEmpty())
                .sorted(Comparator.comparingDouble(StudentPerformance::average).reversed()
                        .thenComparing(performance -> performance.student().fullName()))
                .collect(Collectors.toList());

        double courseAverage = round(ranking.stream()
                .mapToDouble(StudentPerformance::average)
                .average()
                .orElse(0.0));

        Map<String, Long> counted = ranking.stream()
                .collect(Collectors.groupingBy(StudentPerformance::level, Collectors.counting()));
        Map<String, Long> levelCounts = new LinkedHashMap<>();
        LEVELS.forEach(level -> levelCounts.put(level, counted.getOrDefault(level, 0L)));

        return new PerformanceReport(grade, StudentSearchController.ACADEMIC_YEAR, LocalDateTime.now().withNano(0),
                ranking, summarizeSubjects(ranking), courseAverage, levelCounts);
    }

    /** Average per subject and general average of one student. */
    private StudentPerformance toPerformance(StudentSearchResult student, List<GradeEntry> entries) {
        Map<String, Double> subjectAverages = entries.stream()
                .filter(entry -> entry.studentId() == student.studentId())
                .collect(Collectors.groupingBy(GradeEntry::subject, TreeMap::new,
                        Collectors.averagingDouble(GradeEntry::value)));
        subjectAverages.replaceAll((subject, average) -> round(average));
        double average = round(subjectAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
        return new StudentPerformance(student, subjectAverages, average, levelOf(average));
    }

    /** Course average, best and worst student average of each subject. */
    private List<SubjectSummary> summarizeSubjects(List<StudentPerformance> ranking) {
        return ranking.stream()
                .flatMap(performance -> performance.subjectAverages().keySet().stream())
                .distinct()
                .sorted()
                .map(subject -> {
                    List<StudentPerformance> withSubject = ranking.stream()
                            .filter(performance -> performance.subjectAverages().containsKey(subject))
                            .collect(Collectors.toList());
                    double average = round(withSubject.stream()
                            .mapToDouble(performance -> performance.subjectAverages().get(subject))
                            .average()
                            .orElse(0.0));
                    StudentPerformance best = withSubject.stream()
                            .max(Comparator.comparingDouble(performance -> performance.subjectAverages().get(subject)))
                            .orElseThrow();
                    double worst = withSubject.stream()
                            .mapToDouble(performance -> performance.subjectAverages().get(subject))
                            .min()
                            .orElse(0.0);
                    return new SubjectSummary(subject, average, best.subjectAverages().get(subject), worst,
                            best.student().fullName());
                })
                .collect(Collectors.toList());
    }

    private List<GradeEntry> loadGrades() {
        List<GradeEntry> entries = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_GRADES_2025)) {
            statement.setInt(1, StudentSearchController.ACADEMIC_YEAR);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new GradeEntry(resultSet.getInt("id_estudiante"), resultSet.getString("nombre_materia"),
                            resultSet.getBigDecimal("valor_nota").doubleValue()));
                }
            }
            return entries;
        } catch (SQLException e) {
            ErrorLogger.getInstance().error("PerformanceReportController", "Could not load the 2025 grades", e);
            throw new DataAccessException("Error al consultar las notas. El detalle quedó en error_log.txt.", e);
        }
    }

    /** Performance level using the grade rounded to one decimal, as in the report cards. */
    static String levelOf(double average) {
        double grade = Math.round(average * 10.0) / 10.0;
        if (grade >= 4.6) return "Superior";
        if (grade >= 4.0) return "Alto";
        if (grade >= 3.0) return "Básico";
        return "Bajo";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
