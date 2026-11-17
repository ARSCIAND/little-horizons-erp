package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Data access for daily attendance ({@code asistencias}). */
public class AttendanceDao extends BaseDao {

    private static final String SELECT = """
            SELECT a.*, %s AS estudiante, m.nombre_materia
              FROM asistencias a
              JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
              JOIN materias m ON m.id_materia = a.id_materia
            """.formatted(fullName("e"));

    public AttendanceDao(DatabaseConnection database) {
        super(database);
    }

    /**
     * Inserts the attendance or, if the student already has a mark for that subject and date,
     * overwrites it (one mark per student / subject / day — unique key in the database).
     */
    public void saveOrReplace(AttendanceRecord a) {
        inTransaction("save attendance", connection -> {
            int updated = executeUpdate(connection, """
                    UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, registrado_por = ?
                     WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?
                    """, ps -> {
                ps.setString(1, a.getStatus().getDbValue());
                setNullableString(ps, 2, a.getNotes());
                setNullableInt(ps, 3, a.getTeacherId());
                ps.setInt(4, a.getStudentId());
                ps.setInt(5, a.getSubjectId());
                setDate(ps, 6, a.getDate());
            });
            if (updated == 0) {
                executeInsert(connection, """
                        INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, ps -> {
                    ps.setInt(1, a.getStudentId());
                    ps.setInt(2, a.getSubjectId());
                    setDate(ps, 3, a.getDate());
                    ps.setString(4, a.getStatus().getDbValue());
                    setNullableString(ps, 5, a.getNotes());
                    setNullableInt(ps, 6, a.getTeacherId());
                });
            }
            return updated;
        });
    }

    public List<AttendanceRecord> findByStudent(int studentId, LocalDate from, LocalDate to) {
        return queryList(SELECT + " WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? ORDER BY a.fecha_asistencia, m.nombre_materia",
                ps -> {
                    ps.setInt(1, studentId);
                    setDate(ps, 2, from);
                    setDate(ps, 3, to);
                }, this::map);
    }

    public List<AttendanceRecord> findBySubjectAndDate(int subjectId, LocalDate date) {
        return queryList(SELECT + " WHERE a.id_materia = ? AND a.fecha_asistencia = ? ORDER BY e.primer_apellido", ps -> {
            ps.setInt(1, subjectId);
            setDate(ps, 2, date);
        }, this::map);
    }

    /** All marks with the given status in a date range (input of the absence-pattern report). */
    public List<AttendanceRecord> findByStatusBetween(AttendanceStatus status, LocalDate from, LocalDate to) {
        return queryList(SELECT + " WHERE a.estado_asistencia = ? AND a.fecha_asistencia BETWEEN ? AND ?", ps -> {
            ps.setString(1, status.getDbValue());
            setDate(ps, 2, from);
            setDate(ps, 3, to);
        }, this::map);
    }

    private AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord a = new AttendanceRecord();
        a.setId(rs.getInt("id_asistencia"));
        a.setStudentId(rs.getInt("id_estudiante"));
        a.setStudentName(rs.getString("estudiante"));
        a.setSubjectId(rs.getInt("id_materia"));
        a.setSubjectName(rs.getString("nombre_materia"));
        a.setDate(getLocalDate(rs, "fecha_asistencia"));
        a.setStatus(DbEnum.fromDb(AttendanceStatus.class, rs.getString("estado_asistencia")));
        a.setNotes(rs.getString("observaciones"));
        a.setTeacherId(getNullableInt(rs, "registrado_por"));
        return a;
    }
}
