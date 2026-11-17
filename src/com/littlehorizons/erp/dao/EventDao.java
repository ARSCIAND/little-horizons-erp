package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.EventType;
import com.littlehorizons.erp.model.enums.ParticipationRole;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for {@code eventos} and {@code participantes_evento}. */
public class EventDao extends BaseDao implements CrudDao<SchoolEvent>, DeletableDao {

    private static final String SELECT_EVENT = """
            SELECT ev.*, %s AS organizador,
                   (SELECT COUNT(*) FROM participantes_evento pe WHERE pe.id_evento = ev.id_evento) AS participantes
              FROM eventos ev LEFT JOIN profesores p ON p.id_profesor = ev.id_organizador
            """.formatted(fullName("p"));

    private static final String SELECT_PARTICIPANT = """
            SELECT pe.*, %s AS estudiante, ev.nombre_evento
              FROM participantes_evento pe
              JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante
              JOIN eventos ev ON ev.id_evento = pe.id_evento
            """.formatted(fullName("e"));

    public EventDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return queryOne(SELECT_EVENT + " WHERE ev.id_evento = ?", ps -> ps.setInt(1, id), this::mapEvent);
    }

    @Override
    public List<SchoolEvent> findAll() {
        return queryList(SELECT_EVENT + " ORDER BY ev.fecha_evento", StatementBinder.NONE, this::mapEvent);
    }

    @Override
    public int save(SchoolEvent e) {
        return executeInsert("""
                INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, e));
    }

    @Override
    public void update(SchoolEvent e) {
        executeUpdate("""
                UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, fecha_evento = ?, lugar = ?,
                       tiene_competencia = ?, id_organizador = ?, esta_activo = ? WHERE id_evento = ?
                """, ps -> {
            bind(ps, e);
            ps.setInt(9, e.getId());
        });
    }

    /** Deletes an event and (ON DELETE CASCADE) its participants. */
    @Override
    public boolean deleteById(int id) {
        return executeUpdate("DELETE FROM eventos WHERE id_evento = ?", ps -> ps.setInt(1, id)) > 0;
    }

    // ------------------------------------------------------------------ participants

    public List<EventParticipant> findParticipants(int eventId) {
        return queryList(SELECT_PARTICIPANT + """
                 WHERE pe.id_evento = ?
                 ORDER BY FIELD(pe.posicion_obtenida, '1er Lugar', '2do Lugar', '3er Lugar') = 0, pe.posicion_obtenida, e.primer_apellido
                """, ps -> ps.setInt(1, eventId), this::mapParticipant);
    }

    public List<EventParticipant> findAllParticipations() {
        return queryList(SELECT_PARTICIPANT, StatementBinder.NONE, this::mapParticipant);
    }

    public Optional<EventParticipant> findParticipation(int eventId, int studentId) {
        return queryOne(SELECT_PARTICIPANT + " WHERE pe.id_evento = ? AND pe.id_estudiante = ?", ps -> {
            ps.setInt(1, eventId);
            ps.setInt(2, studentId);
        }, this::mapParticipant);
    }

    public boolean isPositionTaken(int eventId, String position) {
        return queryCount("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND posicion_obtenida = ?", ps -> {
            ps.setInt(1, eventId);
            ps.setString(2, position);
        }) > 0;
    }

    public int addParticipant(EventParticipant p) {
        return executeInsert("""
                INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida, observaciones)
                VALUES (?, ?, ?, ?, ?)
                """, ps -> {
            ps.setInt(1, p.getEventId());
            ps.setInt(2, p.getStudentId());
            ps.setString(3, p.getRole().getDbValue());
            setNullableString(ps, 4, p.getPosition());
            setNullableString(ps, 5, p.getNotes());
        });
    }

    public void updateResult(int participationId, String position, String notes) {
        executeUpdate("UPDATE participantes_evento SET posicion_obtenida = ?, observaciones = COALESCE(?, observaciones) WHERE id_participacion = ?",
                ps -> {
                    setNullableString(ps, 1, position);
                    setNullableString(ps, 2, notes);
                    ps.setInt(3, participationId);
                });
    }

    private void bind(PreparedStatement ps, SchoolEvent e) throws SQLException {
        ps.setString(1, e.getName());
        setNullableString(ps, 2, e.getDescription());
        ps.setString(3, e.getType().getDbValue());
        setDate(ps, 4, e.getDate());
        setNullableString(ps, 5, e.getLocation());
        ps.setBoolean(6, e.isCompetitive());
        setNullableInt(ps, 7, e.getOrganizerId());
        ps.setBoolean(8, e.isActive());
    }

    private SchoolEvent mapEvent(ResultSet rs) throws SQLException {
        SchoolEvent e = new SchoolEvent();
        e.setId(rs.getInt("id_evento"));
        e.setName(rs.getString("nombre_evento"));
        e.setDescription(rs.getString("descripcion"));
        e.setType(DbEnum.fromDb(EventType.class, rs.getString("tipo_evento")));
        e.setDate(getLocalDate(rs, "fecha_evento"));
        e.setLocation(rs.getString("lugar"));
        e.setCompetitive(rs.getBoolean("tiene_competencia"));
        e.setOrganizerId(getNullableInt(rs, "id_organizador"));
        e.setOrganizerName(rs.getString("organizador"));
        e.setActive(rs.getBoolean("esta_activo"));
        e.setParticipantCount(rs.getInt("participantes"));
        return e;
    }

    private EventParticipant mapParticipant(ResultSet rs) throws SQLException {
        EventParticipant p = new EventParticipant();
        p.setId(rs.getInt("id_participacion"));
        p.setEventId(rs.getInt("id_evento"));
        p.setEventName(rs.getString("nombre_evento"));
        p.setStudentId(rs.getInt("id_estudiante"));
        p.setStudentName(rs.getString("estudiante"));
        p.setRole(DbEnum.fromDb(ParticipationRole.class, rs.getString("rol_participacion")));
        p.setPosition(rs.getString("posicion_obtenida"));
        p.setNotes(rs.getString("observaciones"));
        return p;
    }
}
