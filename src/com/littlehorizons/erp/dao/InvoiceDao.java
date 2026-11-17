package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.SchoolInfo;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data access for issued electronic invoices ({@code facturas}).
 * The official script names the file column {@code nombre_archivo_pdf}; it is kept unchanged
 * for compatibility and stores the generated .txt file name.
 */
public class InvoiceDao extends BaseDao {

    private static final String SELECT = """
            SELECT f.*, %s AS estudiante, pg.monto_pagado
              FROM facturas f
              JOIN pagos pg ON pg.id_pago = f.id_pago
              JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante
            """.formatted(fullName("e"));

    public InvoiceDao(DatabaseConnection database) {
        super(database);
    }

    public List<Invoice> findAll() {
        return queryList(SELECT + " ORDER BY f.id_factura DESC", StatementBinder.NONE, this::map);
    }

    public Optional<Invoice> findByPayment(int paymentId) {
        return queryOne(SELECT + " WHERE f.id_pago = ?", ps -> ps.setInt(1, paymentId), this::map);
    }

    /** Next consecutive of the DIAN numbering range (1, 2, 3...). */
    public long nextSequence() {
        return queryCount("""
                SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_factura, '-', -1) AS UNSIGNED)), 0) + 1
                  FROM facturas WHERE numero_factura LIKE ?
                """, ps -> ps.setString(1, SchoolInfo.INVOICE_PREFIX + "-%"));
    }

    public int save(Invoice invoice) {
        return executeInsert("""
                INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setString(1, invoice.getInvoiceNumber());
            ps.setInt(2, invoice.getPaymentId());
            setDate(ps, 3, invoice.getIssueDate());
            ps.setString(4, invoice.getCufe());
            ps.setString(5, invoice.getFileName());
            ps.setString(6, invoice.getFilePath());
        });
    }

    private Invoice map(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id_factura"));
        invoice.setInvoiceNumber(rs.getString("numero_factura"));
        invoice.setPaymentId(rs.getInt("id_pago"));
        invoice.setIssueDate(getLocalDate(rs, "fecha_emision"));
        invoice.setCufe(rs.getString("cufe"));
        invoice.setFileName(rs.getString("nombre_archivo_pdf"));
        invoice.setFilePath(rs.getString("ruta_archivo"));
        invoice.setStudentName(rs.getString("estudiante"));
        invoice.setAmount(getMoney(rs, "monto_pagado"));
        return invoice;
    }
}
