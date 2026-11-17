package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.StudentProfile;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Business rules for students, guardians and yearly enrollments (module 1.1). */
public class StudentController {

    private static final int MIN_AGE = 3;
    private static final int MAX_AGE = 21;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final GradeDao gradeDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;

    public StudentController(StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao,
                             GradeDao gradeDao, SubjectEnrollmentDao subjectEnrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.gradeDao = gradeDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
    }

    public int currentYear() {
        return AppConfig.getInstance().getCurrentAcademicYear();
    }

    public List<Grade> getGrades() {
        return gradeDao.findAll();
    }

    // ------------------------------------------------------------------ queries

    public List<Student> listStudents(StudentStatus status) {
        return status == null ? studentDao.findAll() : studentDao.findByStatus(status);
    }

    public List<Student> search(String term) {
        if (term == null || term.isBlank()) {
            throw new BusinessException("Escriba al menos un carácter para buscar.");
        }
        return studentDao.search(term);
    }

    public Student getStudent(int id) {
        return studentDao.findById(id).orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + id + "."));
    }

    public Optional<Student> findByDocument(String document) {
        return studentDao.findByDocument(document);
    }

    public List<Student> listActiveByGrade(int gradeId, int year) {
        return studentDao.findActiveByGrade(gradeId, year);
    }

    public StudentProfile getProfile(int studentId) {
        Student student = getStudent(studentId);
        return new StudentProfile(student, guardianDao.findByStudent(studentId), enrollmentDao.findByStudent(studentId));
    }

    // ------------------------------------------------------------------ registration

    /**
     * Registers a new student, enrolls them in the chosen grade for the current year and links
     * the guardian (requirement: every student has at least one guardian).
     *
     * @param newGuardian      guardian to create, or {@code null} when {@code existingGuardianId} is used
     * @param existingGuardianId id of an already registered guardian (siblings share guardians)
     */
    public Student registerStudent(Student student, int gradeId, Guardian newGuardian, Integer existingGuardianId) {
        validateStudent(student, null);
        gradeDao.findById(gradeId).orElseThrow(() -> new BusinessException("El grado seleccionado no existe."));
        if (newGuardian == null && existingGuardianId == null) {
            throw new BusinessException("Todo estudiante debe tener al menos un acudiente.");
        }
        int guardianId;
        if (newGuardian != null) {
            guardianId = registerGuardian(newGuardian).getId();
        } else {
            guardianId = guardianDao.findById(existingGuardianId)
                    .orElseThrow(() -> new BusinessException("El acudiente seleccionado no existe.")).getId();
        }
        student.setStatus(StudentStatus.ACTIVE);
        student.setId(studentDao.save(student));

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setGradeId(gradeId);
        enrollment.setAcademicYear(currentYear());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setNotes("Matrícula inicial");
        enrollmentDao.save(enrollment);

        guardianDao.link(student.getId(), guardianId, true, true);
        return getStudent(student.getId());
    }

    public void updateStudent(Student student) {
        validateStudent(student, student.getId());
        studentDao.update(student);
    }

    /**
     * Changes the student status keeping enrollments consistent:
     * withdrawn → current enrollment and subjects withdrawn; graduated → only from grade 11.
     */
    public void changeStatus(int studentId, StudentStatus newStatus, String reason) {
        Student student = getStudent(studentId);
        if (student.getStatus() == newStatus) {
            throw new BusinessException("El estudiante ya se encuentra en estado " + newStatus + ".");
        }
        Optional<Enrollment> current = enrollmentDao.findByStudentAndYear(studentId, currentYear());
        switch (newStatus) {
            case WITHDRAWN -> {
                current.filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE).ifPresent(e ->
                        enrollmentDao.updateStatus(e.getId(), EnrollmentStatus.WITHDRAWN, LocalDate.now(), reason));
                subjectEnrollmentDao.withdrawAll(studentId, currentYear());
            }
            case GRADUATED -> {
                Enrollment enrollment = current.orElseThrow(() ->
                        new BusinessException("El estudiante no tiene matrícula en " + currentYear() + "."));
                int level = gradeDao.findById(enrollment.getGradeId()).map(Grade::getLevel).orElse(-1);
                if (level != 11) {
                    throw new BusinessException("Solo se pueden graduar estudiantes matriculados en grado Once.");
                }
                enrollmentDao.updateStatus(enrollment.getId(), EnrollmentStatus.COMPLETED, null, reason);
            }
            case ACTIVE -> current.filter(e -> e.getStatus() == EnrollmentStatus.WITHDRAWN).ifPresent(e ->
                    enrollmentDao.updateStatus(e.getId(), EnrollmentStatus.ACTIVE, null, reason));
            default -> { /* SUSPENDED keeps the enrollment untouched */ }
        }
        studentDao.updateStatus(studentId, newStatus);
    }

    /** Enrolls an existing student in a grade for a (new) academic year. */
    public void enrollInYear(int studentId, int gradeId, int year) {
        Student student = getStudent(studentId);
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessException("Solo se pueden matricular estudiantes activos (estado actual: " + student.getStatus() + ").");
        }
        if (enrollmentDao.findByStudentAndYear(studentId, year).isPresent()) {
            throw new BusinessException("El estudiante ya tiene matrícula para el año " + year + ".");
        }
        Grade target = gradeDao.findById(gradeId).orElseThrow(() -> new BusinessException("El grado no existe."));
        enrollmentDao.findByStudent(studentId).stream()
                .filter(e -> e.getAcademicYear() < year)
                .max(Comparator.comparingInt(Enrollment::getAcademicYear))
                .flatMap(previous -> gradeDao.findById(previous.getGradeId()))
                .filter(previousGrade -> target.getLevel() < previousGrade.getLevel())
                .ifPresent(previousGrade -> {
                    throw new BusinessException("No se puede matricular en un grado inferior al cursado anteriormente ("
                            + previousGrade.getName() + ").");
                });
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setGradeId(gradeId);
        enrollment.setAcademicYear(year);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollmentDao.save(enrollment);
    }

    /** Physically deletes a student registered by mistake (only when it has no academic or financial history). */
    public void deleteStudent(int studentId) {
        getStudent(studentId);
        if (studentDao.hasHistory(studentId)) {
            throw new BusinessException("El estudiante tiene notas, pagos o asistencias: cambie su estado a 'Retirado' en lugar de eliminarlo.");
        }
        studentDao.deleteById(studentId);
    }

    // ------------------------------------------------------------------ guardians

    public Guardian registerGuardian(Guardian guardian) {
        if (guardianDao.findByDocument(guardian.getDocumentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un acudiente con el documento " + guardian.getDocumentNumber() + ".");
        }
        guardian.setId(guardianDao.save(guardian));
        return guardian;
    }

    public void updateGuardian(Guardian guardian) {
        guardianDao.findByDocument(guardian.getDocumentNumber())
                .filter(other -> !other.getId().equals(guardian.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("El documento ya pertenece a otro acudiente.");
                });
        guardianDao.update(guardian);
    }

    public Optional<Guardian> findGuardianByDocument(String document) {
        return guardianDao.findByDocument(document);
    }

    public Guardian getGuardian(int id) {
        return guardianDao.findById(id).orElseThrow(() -> new BusinessException("No existe un acudiente con ID " + id + "."));
    }

    public List<Guardian> getGuardiansOf(int studentId) {
        return guardianDao.findByStudent(studentId);
    }

    public void linkGuardian(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        getStudent(studentId);
        getGuardian(guardianId);
        if (guardianDao.isLinked(studentId, guardianId)) {
            throw new BusinessException("El acudiente ya está vinculado a este estudiante.");
        }
        guardianDao.link(studentId, guardianId, primary, emergencyContact);
    }

    public void unlinkGuardian(int studentId, int guardianId) {
        if (guardianDao.countByStudent(studentId) <= 1) {
            throw new BusinessException("No se puede desvincular: el estudiante debe conservar al menos un acudiente.");
        }
        if (!guardianDao.unlink(studentId, guardianId)) {
            throw new BusinessException("El acudiente no está vinculado a este estudiante.");
        }
    }

    /** Guardians with the students in their care (siblings grouped), ordered by number of students. */
    public Map<Guardian, List<Student>> guardiansWithStudents() {
        Map<Guardian, List<Student>> result = new LinkedHashMap<>();
        guardianDao.findAll().stream()
                .map(g -> Map.entry(g, studentDao.findByGuardian(g.getId())))
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    // ------------------------------------------------------------------ validation

    private void validateStudent(Student s, Integer currentId) {
        studentDao.findByDocument(s.getDocumentNumber())
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un estudiante con el documento " + s.getDocumentNumber()
                            + " (" + other.getFullName() + ").");
                });
        int age = Period.between(s.getBirthDate(), LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessException("La edad del estudiante (" + age + " años) debe estar entre " + MIN_AGE + " y " + MAX_AGE + " años.");
        }
        if (s.getAdmissionDate().isBefore(s.getBirthDate().plusYears(MIN_AGE))) {
            throw new BusinessException("La fecha de ingreso no es coherente con la fecha de nacimiento.");
        }
    }
}
