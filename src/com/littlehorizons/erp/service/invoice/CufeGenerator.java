package com.littlehorizons.erp.service.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Simulated CUFE (Código Único de Factura Electrónica).
 * <p>
 * Follows the structure defined by DIAN's technical annex: SHA-384 over the concatenation of
 * invoice number, date, time, value before taxes, tax codes and values, total, issuer NIT,
 * customer document, technical key and environment type. The technical key is simulated,
 * so the resulting code is unique per invoice but not valid before DIAN.
 */
public final class CufeGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss'-05:00'");
    private static final String TEST_ENVIRONMENT = "2";

    private CufeGenerator() {
    }

    public static String generate(String invoiceNumber, LocalDateTime issuedAt, BigDecimal subtotal, BigDecimal vat,
                                  BigDecimal total, String issuerNit, String customerDocument, String technicalKey) {
        String source = invoiceNumber
                + issuedAt.format(DATE)
                + issuedAt.format(TIME)
                + money(subtotal)
                + "01" + money(vat)        // IVA
                + "04" + money(BigDecimal.ZERO) // Impuesto al consumo
                + "03" + money(BigDecimal.ZERO) // ICA
                + money(total)
                + issuerNit
                + customerDocument
                + technicalKey
                + TEST_ENVIRONMENT;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 not available", e);
        }
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
