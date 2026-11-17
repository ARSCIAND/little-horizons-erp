package com.littlehorizons.erp.config;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Legal and tax information of the school printed on every electronic invoice.
 * DIAN values (resolution, technical key, ranges) are simulated for academic purposes.
 */
public final class SchoolInfo {

    public static final String LEGAL_NAME = "Colegio Internacional Little Horizons S.A.S.";
    public static final String TRADE_NAME = "Colegio Internacional Little Horizons";
    public static final String NIT = "901.456.789-3";
    public static final String NIT_DIGITS = "9014567893";
    public static final String ADDRESS = "Avenida Libertadores # 10-55, Barrio Caobos, Cúcuta, Norte de Santander";
    public static final String PHONE = "(607) 583 4400";
    public static final String EMAIL = "facturacion@littlehorizons.edu.co";
    public static final String TAX_REGIME = "Responsable de IVA - Persona jurídica";

    public static final String INVOICE_PREFIX = "LHFE";
    public static final String DIAN_RESOLUTION_NUMBER = "18764000123456";
    public static final String DIAN_RESOLUTION_DATE = "2026-01-15";
    public static final String DIAN_RESOLUTION_VALIDITY = "2026-01-15 a 2028-01-15";
    public static final String DIAN_RANGE = "LHFE-000001 a LHFE-100000";
    /** Simulated technical key assigned by DIAN to the numbering range (used in the CUFE). */
    public static final String DIAN_TECHNICAL_KEY = "fc8eac422eba16e22ffd8c6f94b3f40a6e38162c";
    public static final String DIAN_QR_BASE_URL = "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=";

    /** Educational services are excluded from VAT in Colombia; only these concept codes carry 19% VAT. */
    public static final Set<String> TAXABLE_CONCEPT_CODES = Set.of("UNIFORME");
    public static final BigDecimal VAT_RATE = new BigDecimal("0.19");

    private SchoolInfo() {
    }
}
