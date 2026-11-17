package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.SubjectEnrollmentStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Data access for {@code inscripciones_materia} (student ↔ subject per period). */
public class SubjectEnrollmentDao extends BaseDao {

    public SubjectEnrollmentDao(DatabaseConnection database) {
        super(database);
    }

    public boolean exists(int studentId, int subjectId, int year, AcademicPeriod period) {
        return queryCount("""
                SELECT COUNT(*) FROM inscripciones_materia
                 WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico = ?
                """, ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setInt(3, year);
            ps.setString(4, period.getDbValue());
        }) > 0;
    }

    public int save(SubjectEnrollment e) {
        return executeInsert("""
                INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion,
                       fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setInt(1, e.getStudentId());
            ps.setInt(2, e.getSubjectId());
            ps.setInt(3, e.getAcademicYear());
            ps.setString(4, e.getPeriod().getDbValue());
            setNullableString(ps, 5, e.getSection());
            setDate(ps, 6, e.getEnrollmentDate());
            ps.setString(7, e.getStatus().getDbValue());
        });
    }

    public List<SubjectEnrollment> findByStudent(int studentId, int year, AcademicPeriod period) {
        return queryList("""
                SELECT * FROM inscripciones_materia WHERE id_estudiante = ? AND año_academico = ? AND periodo_academico = ?
                """, ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, year);
            ps.setString(3, period.getDbValue());
        }, this::map);
    }

    public int withdrawAll(int studentId, int year) {
        return executeUpdate("""
                UPDATE inscripciones_materia SET estado = 'Retirada'
                 WHERE id_estudiante = ? AND año_academico = ? AND estado = 'Activa'
                """, ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, year);
        });
    }

    private SubjectEnrollment map(ResultSet rs) throws SQLException {
        SubjectEnrollment e = new SubjectEnrollment();
        e.setId(rs.getInt("id_inscripcion"));
        e.setStudentId(rs.getInt("id_estudiante"));
        e.setSubjectId(rs.getInt("id_materia"));
        e.setAcademicYear(rs.getInt("año_academico"));
        e.setPeriod(DbEnum.fromDb(AcademicPeriod.class, rs.getString("periodo_academico")));
        e.setSection(rs.getString("seccion"));
        e.setEnrollmentDate(getLocalDate(rs, "fecha_inscripcion"));
        e.setStatus(DbEnum.fromDb(SubjectEnrollmentStatus.class, rs.getString("estado")));
        return e;
    }
}
