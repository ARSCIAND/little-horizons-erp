package com.littlehorizons.erp.service.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable snapshot with everything needed to render an invoice. Generators only depend on this
 * record (not on DAOs), so new output formats can be added without touching data access code.
 */
public record InvoiceData(
        String invoiceNumber,
        long sequence,
        LocalDateTime issuedAt,
        Customer customer,
        String studentName,
        String studentGrade,
        String receiptNumber,
        String paymentMethod,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal vatRatePercent,
        BigDecimal total,
        String totalInWords,
        String cufe,
        String qrContent,
        String notes) {

    /** Person who pays and receives the invoice (the financially responsible guardian). */
    public record Customer(String fullName, String documentType, String documentNumber, String address, String phone, String email) {
    }

    /** One billed concept. */
    public record Line(String code, String description, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    /** File name required by the statement, e.g. factura_000123. */
    public String baseFileName() {
        return String.format("factura_%06d", sequence);
    }
}
