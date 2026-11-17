package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.MembershipStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for clubs ({@code clubes}) and memberships ({@code membresias_club}). */
public class ClubDao extends BaseDao implements CrudDao<Club> {

    private static final String SELECT_CLUB = """
            SELECT c.*, %s AS supervisor,
                   (SELECT COUNT(*) FROM membresias_club mc WHERE mc.id_club = c.id_club AND mc.estado_membresia = 'Activa') AS miembros
              FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
            """.formatted(fullName("p"));

    private static final String SELECT_MEMBERSHIP = """
            SELECT mc.*, %s AS estudiante, c.nombre_club
              FROM membresias_club mc
              JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante
              JOIN clubes c ON c.id_club = mc.id_club
            """.formatted(fullName("e"));

    public ClubDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Club> findById(int id) {
        return queryOne(SELECT_CLUB + " WHERE c.id_club = ?", ps -> ps.setInt(1, id), this::mapClub);
    }

    @Override
    public List<Club> findAll() {
        return queryList(SELECT_CLUB + " ORDER BY c.esta_activo DESC, c.nombre_club", StatementBinder.NONE, this::mapClub);
    }

    @Override
    public int save(Club c) {
        return executeInsert("""
                INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, c));
    }

    @Override
    public void update(Club c) {
        executeUpdate("""
                UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, capacidad_maxima = ?,
                       horario_reunion = ?, esta_activo = ? WHERE id_club = ?
                """, ps -> {
            bind(ps, c);
            ps.setInt(7, c.getId());
        });
    }

    // ------------------------------------------------------------------ memberships

    public List<ClubMembership> findMembers(int clubId, boolean onlyActive) {
        return queryList(SELECT_MEMBERSHIP + " WHERE mc.id_club = ?" + (onlyActive ? " AND mc.estado_membresia = 'Activa'" : "")
                + " ORDER BY mc.estado_membresia, e.primer_apellido", ps -> ps.setInt(1, clubId), this::mapMembership);
    }

    public List<ClubMembership> findByStudent(int studentId) {
        return queryList(SELECT_MEMBERSHIP + " WHERE mc.id_estudiante = ? ORDER BY mc.fecha_ingreso DESC", ps -> ps.setInt(1, studentId), this::mapMembership);
    }

    public List<ClubMembership> findAllActiveMemberships() {
        return queryList(SELECT_MEMBERSHIP + " WHERE mc.estado_membresia = 'Activa'", StatementBinder.NONE, this::mapMembership);
    }

    public Optional<ClubMembership> findMembershipById(int membershipId) {
        return queryOne(SELECT_MEMBERSHIP + " WHERE mc.id_membresia = ?", ps -> ps.setInt(1, membershipId), this::mapMembership);
    }

    public boolean hasActiveMembership(int studentId, int clubId) {
        return queryCount("SELECT COUNT(*) FROM membresias_club WHERE id_estudiante = ? AND id_club = ? AND estado_membresia = 'Activa'",
                ps -> {
                    ps.setInt(1, studentId);
                    ps.setInt(2, clubId);
                }) > 0;
    }

    /**
     * Adds a member validating the capacity inside a transaction. The club row is locked
     * ({@code SELECT ... FOR UPDATE}) so two simultaneous registrations cannot exceed the limit.
     */
    public int addMember(int studentId, int clubId, LocalDate joinDate) {
        return inTransaction("add club member", connection -> {
            int capacity = queryList(connection, "SELECT capacidad_maxima FROM clubes WHERE id_club = ? FOR UPDATE",
                    ps -> ps.setInt(1, clubId), rs -> rs.getInt(1)).stream().findFirst()
                    .orElseThrow(() -> new BusinessException("El club no existe."));
            long members = queryList(connection, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'",
                    ps -> ps.setInt(1, clubId), rs -> rs.getLong(1)).get(0);
            if (members >= capacity) {
                throw new BusinessException("El club alcanzó su cupo máximo (" + capacity + " estudiantes).");
            }
            return executeInsert(connection, "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) VALUES (?, ?, ?, 'Activa')",
                    ps -> {
                        ps.setInt(1, studentId);
                        ps.setInt(2, clubId);
                        setDate(ps, 3, joinDate);
                    });
        });
    }

    public void withdrawMember(int membershipId, LocalDate date, String reason) {
        executeUpdate("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, motivo_retiro = ? WHERE id_membresia = ?",
                ps -> {
                    setDate(ps, 1, date);
                    setNullableString(ps, 2, reason);
                    ps.setInt(3, membershipId);
                });
    }

    private void bind(PreparedStatement ps, Club c) throws SQLException {
        ps.setString(1, c.getName());
        setNullableString(ps, 2, c.getDescription());
        setNullableInt(ps, 3, c.getSupervisorId());
        ps.setInt(4, c.getCapacity());
        setNullableString(ps, 5, c.getSchedule());
        ps.setBoolean(6, c.isActive());
    }

    private Club mapClub(ResultSet rs) throws SQLException {
        Club c = new Club();
        c.setId(rs.getInt("id_club"));
        c.setName(rs.getString("nombre_club"));
        c.setDescription(rs.getString("descripcion"));
        c.setSupervisorId(getNullableInt(rs, "id_profesor_supervisor"));
        c.setSupervisorName(rs.getString("supervisor"));
        c.setCapacity(rs.getInt("capacidad_maxima"));
        c.setSchedule(rs.getString("horario_reunion"));
        c.setActive(rs.getBoolean("esta_activo"));
        c.setActiveMembers(rs.getInt("miembros"));
        return c;
    }

    private ClubMembership mapMembership(ResultSet rs) throws SQLException {
        ClubMembership m = new ClubMembership();
        m.setId(rs.getInt("id_membresia"));
        m.setStudentId(rs.getInt("id_estudiante"));
        m.setStudentName(rs.getString("estudiante"));
        m.setClubId(rs.getInt("id_club"));
        m.setClubName(rs.getString("nombre_club"));
        m.setJoinDate(getLocalDate(rs, "fecha_ingreso"));
        m.setStatus(DbEnum.fromDb(MembershipStatus.class, rs.getString("estado_membresia")));
        m.setWithdrawalDate(getLocalDate(rs, "fecha_retiro"));
        m.setWithdrawalReason(rs.getString("motivo_retiro"));
        return m;
    }
}
