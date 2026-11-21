package test.controlador;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.ErrorLogger;
import test.modelo.GradeOption;
import test.modelo.StudentSearchResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller of the advanced student search.
 * <p>
 * The database query brings every student with an ACTIVE enrollment in 2025
 * (tables estudiantes + matriculas + grados). The filter by grade is applied
 * afterwards in Java with Stream API: stream() → filter() → collect().
 */
public class StudentSearchController {

    /** Academic year required by the exam. */
    public static final int ACADEMIC_YEAR = 2025;
    private static final String ACTIVE_ENROLLMENT = "Activa";

    private static final String SQL_GRADES = """
            SELECT id_grado, nombre_grado, nivel_numerico
              FROM grados
             ORDER BY nivel_numerico
            """;

    private static final String SQL_ACTIVE_ENROLLMENTS = """
            SELECT e.id_estudiante, e.tipo_documento, e.numero_documento,
                   e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido,
                   g.id_grado, g.nombre_grado, m.año_academico
              FROM estudiantes e
              JOIN matriculas m ON m.id_estudiante = e.id_estudiante
              JOIN grados g ON g.id_grado = m.id_grado
             WHERE m.año_academico = ?
               AND m.estado_matricula = ?
             ORDER BY e.id_estudiante
            """;

    private final DatabaseConnection database;

    public StudentSearchController(DatabaseConnection database) {
        this.database = database;
    }

    /** Grades available in the system, ordered from Preescolar to Once. */
    public List<GradeOption> getGrades() {
        List<GradeOption> grades = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_GRADES);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                grades.add(new GradeOption(resultSet.getInt("id_grado"), resultSet.getString("nombre_grado"),
                        resultSet.getInt("nivel_numerico")));
            }
            return grades;
        } catch (SQLException e) {
            throw failure("Could not load the grades", e);
        }
    }

    /** Every student with an active enrollment in {@value #ACADEMIC_YEAR}, without filtering by grade. */
    public List<StudentSearchResult> loadActiveEnrollments() {
        List<StudentSearchResult> students = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_ACTIVE_ENROLLMENTS)) {
            statement.setInt(1, ACADEMIC_YEAR);
            statement.setString(2, ACTIVE_ENROLLMENT);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(new StudentSearchResult(
                            resultSet.getInt("id_estudiante"),
                            resultSet.getString("tipo_documento"),
                            resultSet.getString("numero_documento"),
                            resultSet.getString("primer_nombre"),
                            resultSet.getString("segundo_nombre"),
                            resultSet.getString("primer_apellido"),
                            resultSet.getString("segundo_apellido"),
                            resultSet.getInt("id_grado"),
                            resultSet.getString("nombre_grado"),
                            resultSet.getInt("año_academico")));
                }
            }
            return students;
        } catch (SQLException e) {
            throw failure("Could not load the active enrollments of " + ACADEMIC_YEAR, e);
        }
    }

    /**
     * Filter by academic grade (the only functional filter of the prototype).
     * Students without an active 2025 enrollment never reach the stream (special case 1).
     */
    public List<StudentSearchResult> searchByGrade(GradeOption grade) {
        return loadActiveEnrollments().stream()
                .filter(student -> student.gradeId() == grade.id())
                .collect(Collectors.toList());
    }

    private DataAccessException failure(String message, SQLException e) {
        ErrorLogger.getInstance().error("StudentSearchController", message, e);
        return new DataAccessException("Error al consultar la base de datos. El detalle quedó en error_log.txt.", e);
    }
}
