package com.littlehorizons.erp.model.report;

/** Course average of one subject in a period (requirement 1.3: "promedio general del curso en cada materia"). */
public record SubjectAverage(String subjectCode, String subjectName, double average, double min, double max, long students) {
}
