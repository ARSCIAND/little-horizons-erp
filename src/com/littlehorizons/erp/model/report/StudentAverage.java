package com.littlehorizons.erp.model.report;

/** Overall average of a student across subjects for a period or year. */
public record StudentAverage(int studentId, String studentName, String gradeName, double average, long subjects) {
}
