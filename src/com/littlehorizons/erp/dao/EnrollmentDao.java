package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for yearly grade enrollments ({@code matriculas}). */
public class EnrollmentDao extends BaseDao {

    private static final String SELECT = """
            SELECT m.*, g.nombre_grado FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
            """;

    public EnrollmentDao(DatabaseConnection database) {
        super(database);
    }

    public List<Enrollment> findByStudent(int studentId) {
        return queryList(SELECT + " WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC", ps -> ps.setInt(1, studentId), this::map);
    }

    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) {
        return queryOne(SELECT + " WHERE m.id_estudiante = ? AND m.año_academico = ?", ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, year);
        }, this::map);
    }

    public int save(Enrollment e) {
        return executeInsert("""
                INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, observaciones)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setInt(1, e.getStudentId());
            ps.setInt(2, e.getGradeId());
            ps.setInt(3, e.getAcademicYear());
            ps.setString(4, e.getStatus().getDbValue());
            setDate(ps, 5, e.getEnrollmentDate());
            setNullableString(ps, 6, e.getNotes());
        });
    }

    public void updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate, String notes) {
        executeUpdate("""
                UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ?, observaciones = COALESCE(?, observaciones)
                 WHERE id_matricula = ?
                """, ps -> {
            ps.setString(1, status.getDbValue());
            setNullableDate(ps, 2, withdrawalDate);
            setNullableString(ps, 3, notes);
            ps.setInt(4, enrollmentId);
        });
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getInt("id_matricula"));
        e.setStudentId(rs.getInt("id_estudiante"));
        e.setGradeId(rs.getInt("id_grado"));
        e.setGradeName(rs.getString("nombre_grado"));
        e.setAcademicYear(rs.getInt("año_academico"));
        e.setStatus(DbEnum.fromDb(EnrollmentStatus.class, rs.getString("estado_matricula")));
        e.setEnrollmentDate(getLocalDate(rs, "fecha_matricula"));
        e.setWithdrawalDate(getLocalDate(rs, "fecha_retiro"));
        e.setNotes(rs.getString("observaciones"));
        return e;
    }
}
