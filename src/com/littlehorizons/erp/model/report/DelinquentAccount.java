package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Student with monthly fees pending for more than one month. */
public record DelinquentAccount(int studentId, String studentName, String gradeName, String guardianName,
                                String guardianPhone, int monthsDue, int monthsPaid, BigDecimal monthlyFee,
                                LocalDate lastPaymentDate) {

    public int monthsPending() {
        return Math.max(0, monthsDue - monthsPaid);
    }

    public BigDecimal amountPending() {
        return monthlyFee.multiply(BigDecimal.valueOf(monthsPending()));
    }
}
