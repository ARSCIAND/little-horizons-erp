package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;

import java.util.List;

/** Aggregated view of a student: personal data, guardians and enrollment history. */
public record StudentProfile(Student student, List<Guardian> guardians, List<Enrollment> enrollments) {
}
