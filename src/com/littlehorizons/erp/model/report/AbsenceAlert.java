package com.littlehorizons.erp.model.report;

import java.time.LocalDate;

/** Student whose unexcused absences in a single week exceed the configured threshold. */
public record AbsenceAlert(int studentId, String studentName, String gradeName, LocalDate weekStart, long absences) {
}
