package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.SubjectEnrollmentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.report.SubjectGradeLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Business rules for subjects, subject enrollment, grades and attendance (modules 1.1, 1.3 and 1.4). */
public class AcademicController {

    private final SubjectDao subjectDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final EnrollmentDao enrollmentDao;
    private final AssignmentDao assignmentDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;

    public AcademicController(SubjectDao subjectDao, StudentDao studentDao, TeacherDao teacherDao, EnrollmentDao enrollmentDao,
                              AssignmentDao assignmentDao, SubjectEnrollmentDao subjectEnrollmentDao,
                              AcademicRecordDao recordDao, AttendanceDao attendanceDao) {
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.enrollmentDao = enrollmentDao;
        this.assignmentDao = assignmentDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
    }

    public int currentYear() {
        return AppConfig.getInstance().getCurrentAcademicYear();
    }

    // ------------------------------------------------------------------ subjects

    public List<Subject> listSubjects(Integer gradeId) {
        return gradeId == null ? subjectDao.findAll() : subjectDao.findByGrade(gradeId, false);
    }

    public Subject getSubject(int id) {
        return subjectDao.findById(id).orElseThrow(() -> new BusinessException("No existe una materia con ID " + id + "."));
    }

    public Subject registerSubject(Subject subject) {
        String code = subject.getCode().toUpperCase();
        if (subjectDao.findByCode(code).isPresent()) {
            throw new BusinessException("Ya existe una materia con el código " + code + ".");
        }
        if (subject.getWeeklyHours() < 1 || subject.getWeeklyHours() > 10) {
            throw new BusinessException("Las horas semanales deben estar entre 1 y 10.");
        }
        subject.setCode(code);
        subject.setActive(true);
        subject.setId(subjectDao.save(subject));
        return subject;
    }

    public void setSubjectActive(int subjectId, boolean active) {
        Subject subject = getSubject(subjectId);
        subject.setActive(active);
        subjectDao.update(subject);
    }

    // ------------------------------------------------------------------ subject enrollment

    /** Enrolls a student in every active subject of their grade for the given period. Returns how many were added. */
    public int enrollStudentInGradeSubjects(int studentId, int year, AcademicPeriod period) {
        requireRegularPeriod(period);
        Enrollment enrollment = activeEnrollment(studentId, year);
        List<Subject> pending = subjectDao.findByGrade(enrollment.getGradeId(), true).stream()
                .filter(subject -> !subjectEnrollmentDao.exists(studentId, subject.getId(), year, period))
                .toList();
        pending.forEach(subject -> subjectEnrollmentDao.save(newSubjectEnrollment(studentId, subject.getId(), year, period)));
        return pending.size();
    }

    /** Enrolls every active student of a grade in all of its subjects for a period. */
    public int enrollGradeInPeriod(int gradeId, int year, AcademicPeriod period) {
        requireRegularPeriod(period);
        List<Student> students = studentDao.findActiveByGrade(gradeId, year);
        if (students.isEmpty()) {
            throw new BusinessException("El grado no tiene estudiantes activos matriculados en " + year + ".");
        }
        return students.stream().mapToInt(s -> enrollStudentInGradeSubjects(s.getId(), year, period)).sum();
    }

    // ------------------------------------------------------------------ grades

    /** Active students of the grade of a subject (roster used to enter grades and attendance). */
    public List<Student> getRoster(int subjectId, int year) {
        return studentDao.findActiveByGrade(getSubject(subjectId).getGradeId(), year);
    }

    /** Teacher in charge of a subject for a year (first assignment), used by default when recording. */
    public Teacher getAssignedTeacher(int subjectId, int year) {
        return assignmentDao.findBySubject(subjectId, year).stream()
                .findFirst()
                .flatMap(a -> teacherDao.findById(a.getTeacherId()))
                .orElseThrow(() -> new BusinessException("La materia no tiene profesor asignado en " + year
                        + ". Asígnelo en 'Gestión de Profesores'."));
    }

    /**
     * Records (or corrects) a grade. Rules: valid 0.0–5.0 value, regular period, student active and
     * enrolled in the subject for the period, and the teacher must be assigned to the subject.
     *
     * @return {@code true} if an existing grade was updated, {@code false} if a new one was created
     */
    public boolean recordGrade(int studentId, int subjectId, Integer teacherId, int year, AcademicPeriod period,
                               BigDecimal grade, String notes) {
        requireRegularPeriod(period);
        validateGradeValue(grade);
        Student student = studentDao.findById(studentId).orElseThrow(() -> new BusinessException("El estudiante no existe."));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessException("No se pueden registrar notas a un estudiante en estado " + student.getStatus() + ".");
        }
        getSubject(subjectId);
        if (!subjectEnrollmentDao.exists(studentId, subjectId, year, period)) {
            throw new BusinessException("El estudiante no está inscrito en la materia para el período " + period
                    + ". Use 'Inscribir estudiante en materias'.");
        }
        Teacher teacher = teacherId == null ? getAssignedTeacher(subjectId, year)
                : teacherDao.findById(teacherId).orElseThrow(() -> new BusinessException("El profesor no existe."));
        boolean assigned = assignmentDao.findByTeacher(teacher.getId(), year).stream()
                .map(TeacherAssignment::getSubjectId)
                .anyMatch(id -> id == subjectId);
        if (!assigned) {
            throw new BusinessException(teacher.getFullName() + " no tiene asignada esta materia en " + year + ".");
        }
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessException("El profesor no está activo (estado: " + teacher.getStatus() + ").");
        }

        AcademicRecord record = recordDao.findOne(studentId, subjectId, year, period).orElseGet(AcademicRecord::new);
        boolean exists = record.getId() != null;
        record.setStudentId(studentId);
        record.setSubjectId(subjectId);
        record.setTeacherId(teacher.getId());
        record.setAcademicYear(year);
        record.setPeriod(period);
        record.setGrade(grade);
        record.setNotes(notes);
        record.setRecordDate(LocalDate.now());
        if (exists) {
            recordDao.update(record);
        } else {
            recordDao.save(record);
        }
        return exists;
    }

    /**
     * Computes the final (definitive) grade of every student of a subject as the average of the four
     * regular periods. Students missing a period are skipped. Returns {processed, skipped}.
     */
    public int[] computeFinalGrades(int subjectId, int year) {
        Teacher teacher = getAssignedTeacher(subjectId, year);
        Map<Integer, List<AcademicRecord>> byStudent = recordDao.findBySubject(subjectId, year).stream()
                .filter(r -> r.getPeriod().isRegular())
                .collect(Collectors.groupingBy(AcademicRecord::getStudentId));
        int processed = 0;
        int skipped = 0;
        for (Map.Entry<Integer, List<AcademicRecord>> entry : byStudent.entrySet()) {
            if (entry.getValue().size() < AcademicPeriod.regularPeriods().length) {
                skipped++;
                continue;
            }
            double average = entry.getValue().stream().mapToDouble(AcademicRecord::getGradeValue).average().orElse(0);
            BigDecimal value = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
            AcademicRecord finalRecord = recordDao.findOne(entry.getKey(), subjectId, year, AcademicPeriod.FINAL).orElseGet(AcademicRecord::new);
            boolean exists = finalRecord.getId() != null;
            finalRecord.setStudentId(entry.getKey());
            finalRecord.setSubjectId(subjectId);
            finalRecord.setTeacherId(teacher.getId());
            finalRecord.setAcademicYear(year);
            finalRecord.setPeriod(AcademicPeriod.FINAL);
            finalRecord.setGrade(value);
            finalRecord.setNotes("Promedio automático de los períodos I a IV");
            finalRecord.setRecordDate(LocalDate.now());
            if (exists) {
                recordDao.update(finalRecord);
            } else {
                recordDao.save(finalRecord);
            }
            processed++;
        }
        return new int[]{processed, skipped};
    }

    /** Grades already recorded for a subject and period, keyed by student id. */
    public Map<Integer, BigDecimal> getGradesForPeriod(int subjectId, int year, AcademicPeriod period) {
        return recordDao.findBySubject(subjectId, year).stream()
                .filter(r -> r.getPeriod() == period)
                .collect(Collectors.toMap(AcademicRecord::getStudentId, AcademicRecord::getGrade));
    }

    /**
     * Report card: grades per period and average per subject (requirement 1.3).
     * The subject average uses the regular periods only; the final grade is shown separately.
     */
    public List<SubjectGradeLine> getReportCard(int studentId, int year) {
        Map<String, List<AcademicRecord>> bySubject = recordDao.findByStudentAndYear(studentId, year).stream()
                .collect(Collectors.groupingBy(r -> r.getSubjectCode() + "|" + r.getSubjectName(), TreeMap::new, Collectors.toList()));
        return bySubject.entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\\|", 2);
                    Map<String, Double> grades = new LinkedHashMap<>();
                    entry.getValue().stream()
                            .sorted(Comparator.comparing(AcademicRecord::getPeriod))
                            .forEach(r -> grades.put(r.getPeriod().getDbValue(), r.getGradeValue()));
                    OptionalDouble regular = entry.getValue().stream()
                            .filter(r -> r.getPeriod().isRegular())
                            .mapToDouble(AcademicRecord::getGradeValue)
                            .average();
                    double average = regular.orElse(grades.getOrDefault(AcademicPeriod.FINAL.getDbValue(), 0.0));
                    return new SubjectGradeLine(key[0], key[1], grades, round(average));
                })
                .toList();
    }

    /** General average of a student in a period (or in the year when {@code period} is null). */
    public double getStudentAverage(int studentId, int year, AcademicPeriod period) {
        return round(recordDao.findByStudentAndYear(studentId, year).stream()
                .filter(r -> period == null ? r.getPeriod().isRegular() : r.getPeriod() == period)
                .mapToDouble(AcademicRecord::getGradeValue)
                .average()
                .orElse(0.0));
    }

    /** Complete academic history grouped by year (requirement: "historial desde su ingreso"). */
    public Map<Integer, List<SubjectGradeLine>> getAcademicHistory(int studentId) {
        return recordDao.findByStudent(studentId).stream()
                .map(AcademicRecord::getAcademicYear)
                .distinct()
                .sorted()
                .collect(Collectors.toMap(year -> year, year -> getReportCard(studentId, year), (a, b) -> a, TreeMap::new));
    }

    // ------------------------------------------------------------------ attendance

    /**
     * Saves the attendance of a whole class for a date. Dates in the future or on Sundays are rejected.
     * Returns how many marks were saved.
     */
    public int recordAttendance(int subjectId, LocalDate date, Map<Integer, AttendanceStatus> marks, Map<Integer, String> notes) {
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("No se puede registrar asistencia en una fecha futura.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException("La fecha seleccionada es domingo (no hay clases).");
        }
        Teacher teacher = getAssignedTeacher(subjectId, date.getYear());
        marks.forEach((studentId, status) -> {
            AttendanceRecord record = new AttendanceRecord();
            record.setStudentId(studentId);
            record.setSubjectId(subjectId);
            record.setDate(date);
            record.setStatus(status);
            record.setNotes(notes.get(studentId));
            record.setTeacherId(teacher.getId());
            attendanceDao.saveOrReplace(record);
        });
        return marks.size();
    }

    public List<AttendanceRecord> getAttendance(int studentId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessException("La fecha final no puede ser anterior a la inicial.");
        }
        return attendanceDao.findByStudent(studentId, from, to);
    }

    /** Count of attendance marks per status (e.g. Presente=20, Ausente=2). */
    public Map<AttendanceStatus, Long> summarizeAttendance(List<AttendanceRecord> records) {
        Map<AttendanceStatus, Long> summary = new EnumMap<>(AttendanceStatus.class);
        Arrays.stream(AttendanceStatus.values()).forEach(status -> summary.put(status, 0L));
        records.forEach(r -> summary.merge(r.getStatus(), 1L, Long::sum));
        return summary;
    }

    // ------------------------------------------------------------------ helpers

    private Enrollment activeEnrollment(int studentId, int year) {
        return enrollmentDao.findByStudentAndYear(studentId, year)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene matrícula activa en " + year + "."));
    }

    private static SubjectEnrollment newSubjectEnrollment(int studentId, int subjectId, int year, AcademicPeriod period) {
        SubjectEnrollment e = new SubjectEnrollment();
        e.setStudentId(studentId);
        e.setSubjectId(subjectId);
        e.setAcademicYear(year);
        e.setPeriod(period);
        e.setSection("A");
        e.setEnrollmentDate(LocalDate.now());
        e.setStatus(SubjectEnrollmentStatus.ACTIVE);
        return e;
    }

    private static void requireRegularPeriod(AcademicPeriod period) {
        if (!period.isRegular()) {
            throw new BusinessException("Use los períodos I a IV; la nota Final se calcula automáticamente.");
        }
    }

    private static void validateGradeValue(BigDecimal grade) {
        if (grade == null || grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(new BigDecimal("5.0")) > 0 || grade.scale() > 1) {
            throw new BusinessException("La nota debe estar entre 0.0 y 5.0 con un decimal.");
        }
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
