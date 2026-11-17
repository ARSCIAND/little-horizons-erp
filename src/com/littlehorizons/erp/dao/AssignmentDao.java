package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.WeekDay;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

/** Data access for teacher assignments ({@code asignaciones_profesor}) and their schedules ({@code horarios_clase}). */
public class AssignmentDao extends BaseDao implements DeletableDao {

    /** Extension table (see database.sql, section 1B). */
    public static final String SCHEDULE_TABLE = "horarios_clase";

    private static final String SELECT_ASSIGNMENT = """
            SELECT a.*, %s AS profesor, m.nombre_materia, g.nombre_grado
              FROM asignaciones_profesor a
              JOIN profesores p ON p.id_profesor = a.id_profesor
              JOIN materias m ON m.id_materia = a.id_materia
              JOIN grados g ON g.id_grado = m.id_grado
            """.formatted(fullName("p"));

    private static final String SELECT_SCHEDULE = """
            SELECT h.*, %s AS profesor, m.nombre_materia, g.nombre_grado
              FROM horarios_clase h
              JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
              JOIN profesores p ON p.id_profesor = a.id_profesor
              JOIN materias m ON m.id_materia = a.id_materia
              JOIN grados g ON g.id_grado = m.id_grado
            """.formatted(fullName("p"));
    private static final String SCHEDULE_ORDER = " ORDER BY FIELD(h.dia_semana,'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'), h.hora_inicio";

    public AssignmentDao(DatabaseConnection database) {
        super(database);
    }

    public Optional<TeacherAssignment> findById(int id) {
        return queryOne(SELECT_ASSIGNMENT + " WHERE a.id_asignacion = ?", ps -> ps.setInt(1, id), this::mapAssignment);
    }

    public List<TeacherAssignment> findByTeacher(int teacherId, int year) {
        return queryList(SELECT_ASSIGNMENT + " WHERE a.id_profesor = ? AND a.año_academico = ? ORDER BY g.nivel_numerico, m.nombre_materia",
                ps -> {
                    ps.setInt(1, teacherId);
                    ps.setInt(2, year);
                }, this::mapAssignment);
    }

    public List<TeacherAssignment> findBySubject(int subjectId, int year) {
        return queryList(SELECT_ASSIGNMENT + " WHERE a.id_materia = ? AND a.año_academico = ? ORDER BY a.seccion", ps -> {
            ps.setInt(1, subjectId);
            ps.setInt(2, year);
        }, this::mapAssignment);
    }

    public boolean exists(int teacherId, int subjectId, int year, String section) {
        return queryCount("""
                SELECT COUNT(*) FROM asignaciones_profesor
                 WHERE id_profesor = ? AND id_materia = ? AND año_academico = ? AND seccion = ?
                """, ps -> {
            ps.setInt(1, teacherId);
            ps.setInt(2, subjectId);
            ps.setInt(3, year);
            ps.setString(4, section);
        }) > 0;
    }

    public int save(TeacherAssignment a) {
        return executeInsert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion) VALUES (?, ?, ?, ?)", ps -> {
            ps.setInt(1, a.getTeacherId());
            ps.setInt(2, a.getSubjectId());
            ps.setInt(3, a.getAcademicYear());
            ps.setString(4, a.getSection());
        });
    }

    // ------------------------------------------------------------------ schedules

    public boolean schedulesAvailable() {
        return tableExists(SCHEDULE_TABLE);
    }

    public List<ClassSchedule> findSchedulesByTeacher(int teacherId, int year) {
        if (!schedulesAvailable()) {
            return List.of();
        }
        return queryList(SELECT_SCHEDULE + " WHERE a.id_profesor = ? AND a.año_academico = ?" + SCHEDULE_ORDER, ps -> {
            ps.setInt(1, teacherId);
            ps.setInt(2, year);
        }, this::mapSchedule);
    }

    public List<ClassSchedule> findSchedulesByGrade(int gradeId, int year) {
        if (!schedulesAvailable()) {
            return List.of();
        }
        return queryList(SELECT_SCHEDULE + " WHERE m.id_grado = ? AND a.año_academico = ?" + SCHEDULE_ORDER, ps -> {
            ps.setInt(1, gradeId);
            ps.setInt(2, year);
        }, this::mapSchedule);
    }

    public int saveSchedule(ClassSchedule s) {
        return executeInsert("INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin, aula) VALUES (?, ?, ?, ?, ?)", ps -> {
            ps.setInt(1, s.getAssignmentId());
            ps.setString(2, s.getDay().getDbValue());
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            setNullableString(ps, 5, s.getRoom());
        });
    }

    /** Deletes a schedule slot. */
    @Override
    public boolean deleteById(int scheduleId) {
        return executeUpdate("DELETE FROM horarios_clase WHERE id_horario = ?", ps -> ps.setInt(1, scheduleId)) > 0;
    }

    private TeacherAssignment mapAssignment(ResultSet rs) throws SQLException {
        TeacherAssignment a = new TeacherAssignment();
        a.setId(rs.getInt("id_asignacion"));
        a.setTeacherId(rs.getInt("id_profesor"));
        a.setTeacherName(rs.getString("profesor"));
        a.setSubjectId(rs.getInt("id_materia"));
        a.setSubjectName(rs.getString("nombre_materia"));
        a.setGradeName(rs.getString("nombre_grado"));
        a.setAcademicYear(rs.getInt("año_academico"));
        a.setSection(rs.getString("seccion"));
        return a;
    }

    private ClassSchedule mapSchedule(ResultSet rs) throws SQLException {
        ClassSchedule s = new ClassSchedule();
        s.setId(rs.getInt("id_horario"));
        s.setAssignmentId(rs.getInt("id_asignacion"));
        s.setDay(DbEnum.fromDb(WeekDay.class, rs.getString("dia_semana")));
        s.setStartTime(rs.getTime("hora_inicio").toLocalTime());
        s.setEndTime(rs.getTime("hora_fin").toLocalTime());
        s.setRoom(rs.getString("aula"));
        s.setTeacherName(rs.getString("profesor"));
        s.setSubjectName(rs.getString("nombre_materia"));
        s.setGradeName(rs.getString("nombre_grado"));
        return s;
    }
}
