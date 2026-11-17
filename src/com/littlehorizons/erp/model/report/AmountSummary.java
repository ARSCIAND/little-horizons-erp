package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;

/** Generic "label / count / total" aggregate used by the financial reports. */
public record AmountSummary(String label, long count, BigDecimal total) {
}
