package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.CustomPrice;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for {@code conceptos_pago} and student specific values ({@code valores_personalizados}). */
public class PaymentConceptDao extends BaseDao implements CrudDao<PaymentConcept> {

    public PaymentConceptDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return queryOne("SELECT * FROM conceptos_pago WHERE id_concepto = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<PaymentConcept> findAll() {
        return queryList("SELECT * FROM conceptos_pago ORDER BY esta_activo DESC, id_concepto", StatementBinder.NONE, this::map);
    }

    public List<PaymentConcept> findActive() {
        return queryList("SELECT * FROM conceptos_pago WHERE esta_activo = TRUE ORDER BY id_concepto", StatementBinder.NONE, this::map);
    }

    public Optional<PaymentConcept> findByCode(String code) {
        return queryOne("SELECT * FROM conceptos_pago WHERE codigo_concepto = ?", ps -> ps.setString(1, code), this::map);
    }

    @Override
    public int save(PaymentConcept c) {
        return executeInsert("""
                INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, c));
    }

    @Override
    public void update(PaymentConcept c) {
        executeUpdate("""
                UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, descripcion = ?, monto_base = ?,
                       es_recurrente = ?, esta_activo = ? WHERE id_concepto = ?
                """, ps -> {
            bind(ps, c);
            ps.setInt(7, c.getId());
        });
    }

    // ------------------------------------------------------------------ custom prices

    private static final String SELECT_CUSTOM = """
            SELECT v.*, %s AS estudiante, c.nombre_concepto
              FROM valores_personalizados v
              JOIN estudiantes e ON e.id_estudiante = v.id_estudiante
              JOIN conceptos_pago c ON c.id_concepto = v.id_concepto
            """.formatted(fullName("e"));

    /** Extension table (see database.sql, section 1B). */
    public static final String CUSTOM_PRICE_TABLE = "valores_personalizados";

    public boolean customPricesAvailable() {
        return tableExists(CUSTOM_PRICE_TABLE);
    }

    public Optional<CustomPrice> findCustomPrice(int studentId, int conceptId, int year) {
        if (!customPricesAvailable()) {
            return Optional.empty();
        }
        return queryOne(SELECT_CUSTOM + " WHERE v.id_estudiante = ? AND v.id_concepto = ? AND v.año_academico = ?", ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, conceptId);
            ps.setInt(3, year);
        }, this::mapCustom);
    }

    public List<CustomPrice> findCustomPrices(int year) {
        if (!customPricesAvailable()) {
            return List.of();
        }
        return queryList(SELECT_CUSTOM + " WHERE v.año_academico = ? ORDER BY e.primer_apellido", ps -> ps.setInt(1, year), this::mapCustom);
    }

    /** Creates or replaces the custom value of a concept for a student and year. */
    public void saveCustomPrice(CustomPrice p) {
        inTransaction("save custom price", connection -> {
            int updated = executeUpdate(connection, """
                    UPDATE valores_personalizados SET monto_personalizado = ?, motivo = ?
                     WHERE id_estudiante = ? AND id_concepto = ? AND año_academico = ?
                    """, ps -> {
                ps.setBigDecimal(1, p.getAmount());
                setNullableString(ps, 2, p.getReason());
                ps.setInt(3, p.getStudentId());
                ps.setInt(4, p.getConceptId());
                ps.setInt(5, p.getAcademicYear());
            });
            if (updated == 0) {
                executeInsert(connection, """
                        INSERT INTO valores_personalizados (id_estudiante, id_concepto, monto_personalizado, motivo, año_academico)
                        VALUES (?, ?, ?, ?, ?)
                        """, ps -> {
                    ps.setInt(1, p.getStudentId());
                    ps.setInt(2, p.getConceptId());
                    ps.setBigDecimal(3, p.getAmount());
                    setNullableString(ps, 4, p.getReason());
                    ps.setInt(5, p.getAcademicYear());
                });
            }
            return updated;
        });
    }

    public boolean deleteCustomPrice(int customPriceId) {
        return executeUpdate("DELETE FROM valores_personalizados WHERE id_valor = ?", ps -> ps.setInt(1, customPriceId)) > 0;
    }

    private void bind(PreparedStatement ps, PaymentConcept c) throws SQLException {
        ps.setString(1, c.getCode());
        ps.setString(2, c.getName());
        setNullableString(ps, 3, c.getDescription());
        ps.setBigDecimal(4, c.getBaseAmount());
        ps.setBoolean(5, c.isRecurring());
        ps.setBoolean(6, c.isActive());
    }

    private PaymentConcept map(ResultSet rs) throws SQLException {
        PaymentConcept c = new PaymentConcept();
        c.setId(rs.getInt("id_concepto"));
        c.setCode(rs.getString("codigo_concepto"));
        c.setName(rs.getString("nombre_concepto"));
        c.setDescription(rs.getString("descripcion"));
        c.setBaseAmount(getMoney(rs, "monto_base"));
        c.setRecurring(rs.getBoolean("es_recurrente"));
        c.setActive(rs.getBoolean("esta_activo"));
        return c;
    }

    private CustomPrice mapCustom(ResultSet rs) throws SQLException {
        CustomPrice p = new CustomPrice();
        p.setId(rs.getInt("id_valor"));
        p.setStudentId(rs.getInt("id_estudiante"));
        p.setStudentName(rs.getString("estudiante"));
        p.setConceptId(rs.getInt("id_concepto"));
        p.setConceptName(rs.getString("nombre_concepto"));
        p.setAmount(getMoney(rs, "monto_personalizado"));
        p.setReason(rs.getString("motivo"));
        p.setAcademicYear(rs.getInt("año_academico"));
        return p;
    }
}
