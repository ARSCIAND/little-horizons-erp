package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for grades ({@code registros_academicos}). Averages are computed in the controller with streams. */
public class AcademicRecordDao extends BaseDao implements CrudDao<AcademicRecord> {

    private static final String SELECT = """
            SELECT r.*, %s AS estudiante, m.codigo_materia, m.nombre_materia, %s AS profesor
              FROM registros_academicos r
              JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
              JOIN materias m ON m.id_materia = r.id_materia
              JOIN profesores p ON p.id_profesor = r.id_profesor
            """.formatted(fullName("e"), fullName("p"));

    public AcademicRecordDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<AcademicRecord> findById(int id) {
        return queryOne(SELECT + " WHERE r.id_registro = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<AcademicRecord> findAll() {
        return queryList(SELECT + " ORDER BY r.año_academico, r.id_estudiante", StatementBinder.NONE, this::map);
    }

    /** Full academic history of a student since admission. */
    public List<AcademicRecord> findByStudent(int studentId) {
        return queryList(SELECT + " WHERE r.id_estudiante = ? ORDER BY r.año_academico, m.nombre_materia, r.periodo_academico",
                ps -> ps.setInt(1, studentId), this::map);
    }

    public List<AcademicRecord> findByStudentAndYear(int studentId, int year) {
        return queryList(SELECT + " WHERE r.id_estudiante = ? AND r.año_academico = ? ORDER BY m.nombre_materia, r.periodo_academico",
                ps -> {
                    ps.setInt(1, studentId);
                    ps.setInt(2, year);
                }, this::map);
    }

    public List<AcademicRecord> findBySubject(int subjectId, int year) {
        return queryList(SELECT + " WHERE r.id_materia = ? AND r.año_academico = ? ORDER BY e.primer_apellido, r.periodo_academico",
                ps -> {
                    ps.setInt(1, subjectId);
                    ps.setInt(2, year);
                }, this::map);
    }

    /** Every grade of every subject of a grade level in a year (input for course statistics). */
    public List<AcademicRecord> findByGrade(int gradeId, int year) {
        return queryList(SELECT + " WHERE m.id_grado = ? AND r.año_academico = ?", ps -> {
            ps.setInt(1, gradeId);
            ps.setInt(2, year);
        }, this::map);
    }

    public Optional<AcademicRecord> findOne(int studentId, int subjectId, int year, AcademicPeriod period) {
        return queryOne(SELECT + " WHERE r.id_estudiante = ? AND r.id_materia = ? AND r.año_academico = ? AND r.periodo_academico = ?",
                ps -> {
                    ps.setInt(1, studentId);
                    ps.setInt(2, subjectId);
                    ps.setInt(3, year);
                    ps.setString(4, period.getDbValue());
                }, this::map);
    }

    @Override
    public int save(AcademicRecord r) {
        return executeInsert("""
                INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico,
                       valor_nota, observaciones, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setInt(1, r.getStudentId());
            ps.setInt(2, r.getSubjectId());
            ps.setInt(3, r.getTeacherId());
            ps.setInt(4, r.getAcademicYear());
            ps.setString(5, r.getPeriod().getDbValue());
            ps.setBigDecimal(6, r.getGrade());
            setNullableString(ps, 7, r.getNotes());
            setDate(ps, 8, r.getRecordDate());
        });
    }

    /** Corrects an existing grade (value, teacher, notes, date). */
    @Override
    public void update(AcademicRecord r) {
        executeUpdate("""
                UPDATE registros_academicos SET valor_nota = ?, id_profesor = ?, observaciones = ?, fecha_registro = ?
                 WHERE id_registro = ?
                """, ps -> {
            ps.setBigDecimal(1, r.getGrade());
            ps.setInt(2, r.getTeacherId());
            setNullableString(ps, 3, r.getNotes());
            setDate(ps, 4, r.getRecordDate());
            ps.setInt(5, r.getId());
        });
    }

    private AcademicRecord map(ResultSet rs) throws SQLException {
        AcademicRecord r = new AcademicRecord();
        r.setId(rs.getInt("id_registro"));
        r.setStudentId(rs.getInt("id_estudiante"));
        r.setStudentName(rs.getString("estudiante"));
        r.setSubjectId(rs.getInt("id_materia"));
        r.setSubjectCode(rs.getString("codigo_materia"));
        r.setSubjectName(rs.getString("nombre_materia"));
        r.setTeacherId(rs.getInt("id_profesor"));
        r.setTeacherName(rs.getString("profesor"));
        r.setAcademicYear(rs.getInt("año_academico"));
        r.setPeriod(DbEnum.fromDb(AcademicPeriod.class, rs.getString("periodo_academico")));
        r.setGrade(rs.getBigDecimal("valor_nota"));
        r.setNotes(rs.getString("observaciones"));
        r.setRecordDate(getLocalDate(rs, "fecha_registro"));
        return r;
    }
}
