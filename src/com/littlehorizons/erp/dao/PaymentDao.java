package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.report.AmountSummary;
import com.littlehorizons.erp.model.report.DelinquentAccount;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for payments ({@code pagos}) and the financial aggregates used by reports. */
public class PaymentDao extends BaseDao implements CrudDao<Payment> {

    private static final String SELECT = """
            SELECT pg.*, %s AS estudiante, %s AS acudiente, c.codigo_concepto, c.nombre_concepto, f.numero_factura
              FROM pagos pg
              JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante
              JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto
              LEFT JOIN acudientes a ON a.id_acudiente = pg.id_acudiente
              LEFT JOIN facturas f ON f.id_pago = pg.id_pago
            """.formatted(fullName("e"), fullName("a"));
    private static final String ORDER = " ORDER BY pg.fecha_pago DESC, pg.id_pago DESC";

    public PaymentDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Payment> findById(int id) {
        return queryOne(SELECT + " WHERE pg.id_pago = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<Payment> findAll() {
        return queryList(SELECT + ORDER, StatementBinder.NONE, this::map);
    }

    public Optional<Payment> findByReceipt(String receiptNumber) {
        return queryOne(SELECT + " WHERE pg.numero_recibo = ?", ps -> ps.setString(1, receiptNumber), this::map);
    }

    public List<Payment> findByStudent(int studentId) {
        return queryList(SELECT + " WHERE pg.id_estudiante = ?" + ORDER, ps -> ps.setInt(1, studentId), this::map);
    }

    /** Payments of every student under the guardian's care (siblings) plus any payment the guardian made. */
    public List<Payment> findByGuardian(int guardianId) {
        return queryList(SELECT + """
                 WHERE pg.id_acudiente = ?
                    OR pg.id_estudiante IN (SELECT id_estudiante FROM estudiante_acudiente WHERE id_acudiente = ?)
                """ + ORDER, ps -> {
            ps.setInt(1, guardianId);
            ps.setInt(2, guardianId);
        }, this::map);
    }

    public List<Payment> findByYear(int year) {
        return queryList(SELECT + " WHERE pg.año_academico = ?" + ORDER, ps -> ps.setInt(1, year), this::map);
    }

    public List<Payment> findWithoutInvoice(int limit) {
        return queryList(SELECT + " WHERE f.id_factura IS NULL" + ORDER + " LIMIT ?", ps -> ps.setInt(1, limit), this::map);
    }

    /** Next sequential receipt number for the year, e.g. RC-2026-000605. */
    public String nextReceiptNumber(int year) {
        long next = queryCount("""
                SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_recibo, '-', -1) AS UNSIGNED)), 0) + 1
                  FROM pagos WHERE numero_recibo LIKE ?
                """, ps -> ps.setString(1, "RC-" + year + "-%"));
        return String.format("RC-%d-%06d", year, next);
    }

    @Override
    public int save(Payment p) {
        return executeInsert("""
                INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago,
                       numero_recibo, año_academico, periodo_academico, notas)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setInt(1, p.getStudentId());
            setNullableInt(ps, 2, p.getGuardianId());
            ps.setInt(3, p.getConceptId());
            ps.setBigDecimal(4, p.getAmount());
            setDate(ps, 5, p.getPaymentDate());
            ps.setString(6, p.getMethod().getDbValue());
            ps.setString(7, p.getReceiptNumber());
            setNullableInt(ps, 8, p.getAcademicYear());
            ps.setString(9, p.getPeriod() == null ? null : p.getPeriod().getDbValue());
            setNullableString(ps, 10, p.getNotes());
        });
    }

    /** Only notes and method can be corrected; amounts are immutable once invoiced. */
    @Override
    public void update(Payment p) {
        executeUpdate("UPDATE pagos SET metodo_pago = ?, notas = ? WHERE id_pago = ?", ps -> {
            ps.setString(1, p.getMethod().getDbValue());
            setNullableString(ps, 2, p.getNotes());
            ps.setInt(3, p.getId());
        });
    }

    /**
     * Monthly fee status of every active student of the year: fee value (custom or base),
     * number of monthly fees paid and last payment date. {@code monthsDue} is supplied by the caller.
     */
    public List<DelinquentAccount> findPensionStatus(int year, int monthsDue) {
        // valores_personalizados is an optional extension table (database.sql section 1B)
        boolean withCustom = tableExists("valores_personalizados");
        String fee = withCustom ? "COALESCE(v.monto_personalizado, c.monto_base)" : "c.monto_base";
        String customJoin = withCustom
                ? "LEFT JOIN valores_personalizados v ON v.id_estudiante = e.id_estudiante"
                + " AND v.id_concepto = c.id_concepto AND v.año_academico = ?"
                : "";
        return queryList("""
                SELECT e.id_estudiante, %s AS estudiante, g.nombre_grado,
                       %s AS cuota,
                       COUNT(pg.id_pago) AS pagadas, MAX(pg.fecha_pago) AS ultimo_pago,
                       (SELECT %s FROM estudiante_acudiente ea JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
                         WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC LIMIT 1) AS acudiente,
                       (SELECT a.telefono_principal FROM estudiante_acudiente ea JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
                         WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC LIMIT 1) AS telefono
                  FROM estudiantes e
                  JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = ? AND m.estado_matricula = 'Activa'
                  JOIN grados g ON g.id_grado = m.id_grado
                  JOIN conceptos_pago c ON c.codigo_concepto = 'PENSION'
                  %s
                  LEFT JOIN pagos pg ON pg.id_estudiante = e.id_estudiante AND pg.id_concepto = c.id_concepto AND pg.año_academico = ?
                 WHERE e.estado = 'Activo'
                 GROUP BY e.id_estudiante, estudiante, g.nombre_grado, g.nivel_numerico, cuota
                 ORDER BY g.nivel_numerico, estudiante
                """.formatted(fullName("e"), fee, fullName("a"), customJoin), ps -> {
            int index = 1;
            ps.setInt(index++, year);
            if (withCustom) {
                ps.setInt(index++, year);
            }
            ps.setInt(index, year);
        }, rs -> new DelinquentAccount(rs.getInt("id_estudiante"), rs.getString("estudiante"), rs.getString("nombre_grado"),
                rs.getString("acudiente"), rs.getString("telefono"), monthsDue, rs.getInt("pagadas"),
                getMoney(rs, "cuota"), getLocalDate(rs, "ultimo_pago")));
    }

    public List<AmountSummary> sumByConcept(int year) {
        return queryList("""
                SELECT c.nombre_concepto AS etiqueta, COUNT(*) AS cantidad, SUM(pg.monto_pagado) AS total
                  FROM pagos pg JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto
                 WHERE pg.año_academico = ? GROUP BY c.nombre_concepto ORDER BY total DESC
                """, ps -> ps.setInt(1, year), this::mapSummary);
    }

    public List<AmountSummary> sumByMethod(int year) {
        return queryList("""
                SELECT metodo_pago AS etiqueta, COUNT(*) AS cantidad, SUM(monto_pagado) AS total
                  FROM pagos WHERE año_academico = ? GROUP BY metodo_pago ORDER BY total DESC
                """, ps -> ps.setInt(1, year), this::mapSummary);
    }

    private AmountSummary mapSummary(ResultSet rs) throws SQLException {
        return new AmountSummary(rs.getString("etiqueta"), rs.getLong("cantidad"), getMoney(rs, "total"));
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id_pago"));
        p.setStudentId(rs.getInt("id_estudiante"));
        p.setStudentName(rs.getString("estudiante"));
        p.setGuardianId(getNullableInt(rs, "id_acudiente"));
        p.setGuardianName(rs.getString("acudiente"));
        p.setConceptId(rs.getInt("id_concepto"));
        p.setConceptCode(rs.getString("codigo_concepto"));
        p.setConceptName(rs.getString("nombre_concepto"));
        p.setAmount(getMoney(rs, "monto_pagado"));
        p.setPaymentDate(getLocalDate(rs, "fecha_pago"));
        p.setMethod(DbEnum.fromDb(PaymentMethod.class, rs.getString("metodo_pago")));
        p.setReceiptNumber(rs.getString("numero_recibo"));
        p.setAcademicYear(getNullableInt(rs, "año_academico"));
        String period = rs.getString("periodo_academico");
        p.setPeriod(period == null ? null : DbEnum.fromDb(PaymentPeriod.class, period));
        p.setNotes(rs.getString("notas"));
        p.setInvoiceNumber(rs.getString("numero_factura"));
        return p;
    }
}
