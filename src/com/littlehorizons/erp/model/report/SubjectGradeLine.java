package com.littlehorizons.erp.model.report;

import java.util.Map;

/**
 * One row of a student's report card: grades per period for a subject and the computed
 * subject average (requirement 1.3: "promedio por materia de cada estudiante").
 */
public record SubjectGradeLine(String subjectCode, String subjectName, Map<String, Double> gradesByPeriod, double average) {
}
