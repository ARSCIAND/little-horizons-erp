package com.littlehorizons.erp.model.report;

import java.util.List;

/** Student who needs academic support: low overall average or failing subjects (grade below 3.0). */
public record SupportCase(int studentId, String studentName, String gradeName, double average, List<String> failingSubjects) {
}
