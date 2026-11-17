package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.SubjectEnrollmentStatus;
import java.time.LocalDate;

/**
 * Registration of a student in a subject for a given year and period.
 */
public class SubjectEnrollment {

    private Integer id;
    private int studentId;
    private int subjectId;
    private int academicYear;
    private AcademicPeriod period;
    private String section;
    private LocalDate enrollmentDate;
    private SubjectEnrollmentStatus status;

    public SubjectEnrollment() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public AcademicPeriod getPeriod() {
        return period;
    }

    public void setPeriod(AcademicPeriod period) {
        this.period = period;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public SubjectEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(SubjectEnrollmentStatus status) {
        this.status = status;
    }
}
