package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for {@code acudientes} and the many-to-many table {@code estudiante_acudiente}. */
public class GuardianDao extends BaseDao implements CrudDao<Guardian> {

    private static final String SELECT = "SELECT a.*, FALSE AS es_principal FROM acudientes a";

    public GuardianDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Guardian> findById(int id) {
        return queryOne(SELECT + " WHERE a.id_acudiente = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<Guardian> findAll() {
        return queryList(SELECT + " ORDER BY a.primer_apellido, a.primer_nombre", StatementBinder.NONE, this::map);
    }

    public Optional<Guardian> findByDocument(String documentNumber) {
        return queryOne(SELECT + " WHERE a.numero_documento = ?", ps -> ps.setString(1, documentNumber), this::map);
    }

    /** Guardians of a student; the primary contact comes first. */
    public List<Guardian> findByStudent(int studentId) {
        return queryList("""
                SELECT a.*, ea.es_principal FROM acudientes a
                  JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente
                 WHERE ea.id_estudiante = ?
                 ORDER BY ea.es_principal DESC, a.es_responsable_financiero DESC, a.primer_apellido
                """, ps -> ps.setInt(1, studentId), this::map);
    }

    /** Guardian billed for a student: financially responsible, preferring the primary contact. */
    public Optional<Guardian> findPayer(int studentId) {
        return findByStudent(studentId).stream()
                .filter(Guardian::isFinanciallyResponsible)
                .findFirst()
                .or(() -> findByStudent(studentId).stream().findFirst());
    }

    @Override
    public int save(Guardian g) {
        return executeInsert("""
                INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                       segundo_apellido, parentesco, telefono_principal, telefono_secundario, correo_electronico, direccion,
                       ocupacion, es_responsable_financiero)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, g));
    }

    @Override
    public void update(Guardian g) {
        executeUpdate("""
                UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                       primer_apellido = ?, segundo_apellido = ?, parentesco = ?, telefono_principal = ?, telefono_secundario = ?,
                       correo_electronico = ?, direccion = ?, ocupacion = ?, es_responsable_financiero = ?
                 WHERE id_acudiente = ?
                """, ps -> {
            bind(ps, g);
            ps.setInt(14, g.getId());
        });
    }

    public boolean isLinked(int studentId, int guardianId) {
        return queryCount("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?", ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, guardianId);
        }) > 0;
    }

    public long countByStudent(int studentId) {
        return queryCount("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ?", ps -> ps.setInt(1, studentId));
    }

    /** Links a guardian with a student. When {@code primary} is true any previous primary flag is cleared. */
    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        inTransaction("link guardian", connection -> {
            if (primary) {
                executeUpdate(connection, "UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?",
                        ps -> ps.setInt(1, studentId));
            }
            return executeUpdate(connection, """
                    INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia)
                    VALUES (?, ?, ?, ?)
                    """, ps -> {
                ps.setInt(1, studentId);
                ps.setInt(2, guardianId);
                ps.setBoolean(3, primary);
                ps.setBoolean(4, emergencyContact);
            });
        });
    }

    public boolean unlink(int studentId, int guardianId) {
        return executeUpdate("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?", ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, guardianId);
        }) > 0;
    }

    private void bind(PreparedStatement ps, Guardian g) throws SQLException {
        ps.setString(1, g.getDocumentType().getDbValue());
        ps.setString(2, g.getDocumentNumber());
        ps.setString(3, g.getFirstName());
        setNullableString(ps, 4, g.getMiddleName());
        ps.setString(5, g.getLastName());
        setNullableString(ps, 6, g.getSecondLastName());
        ps.setString(7, g.getRelationship());
        ps.setString(8, g.getPhone());
        setNullableString(ps, 9, g.getSecondaryPhone());
        setNullableString(ps, 10, g.getEmail());
        setNullableString(ps, 11, g.getAddress());
        setNullableString(ps, 12, g.getOccupation());
        ps.setBoolean(13, g.isFinanciallyResponsible());
    }

    private Guardian map(ResultSet rs) throws SQLException {
        Guardian g = new Guardian();
        g.setId(rs.getInt("id_acudiente"));
        g.setDocumentType(DbEnum.fromDb(DocumentType.class, rs.getString("tipo_documento")));
        g.setDocumentNumber(rs.getString("numero_documento"));
        g.setFirstName(rs.getString("primer_nombre"));
        g.setMiddleName(rs.getString("segundo_nombre"));
        g.setLastName(rs.getString("primer_apellido"));
        g.setSecondLastName(rs.getString("segundo_apellido"));
        g.setRelationship(rs.getString("parentesco"));
        g.setPhone(rs.getString("telefono_principal"));
        g.setSecondaryPhone(rs.getString("telefono_secundario"));
        g.setEmail(rs.getString("correo_electronico"));
        g.setAddress(rs.getString("direccion"));
        g.setOccupation(rs.getString("ocupacion"));
        g.setFinanciallyResponsible(rs.getBoolean("es_responsable_financiero"));
        g.setPrimaryContact(rs.getBoolean("es_principal"));
        return g;
    }
}
