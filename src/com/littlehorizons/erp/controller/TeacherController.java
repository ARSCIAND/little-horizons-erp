package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.enums.WeekDay;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Business rules for teachers, subject assignments and class schedules (module 1.2). */
public class TeacherController {

    private static final LocalTime SCHOOL_OPENS = LocalTime.of(6, 0);
    private static final LocalTime SCHOOL_CLOSES = LocalTime.of(18, 0);

    private final TeacherDao teacherDao;
    private final AssignmentDao assignmentDao;
    private final SubjectDao subjectDao;

    public TeacherController(TeacherDao teacherDao, AssignmentDao assignmentDao, SubjectDao subjectDao) {
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
    }

    public int currentYear() {
        return AppConfig.getInstance().getCurrentAcademicYear();
    }

    public List<Teacher> listTeachers(TeacherStatus status) {
        return status == null ? teacherDao.findAll() : teacherDao.findByStatus(status);
    }

    public List<Teacher> search(String term) {
        return teacherDao.search(term);
    }

    public Teacher getTeacher(int id) {
        return teacherDao.findById(id).orElseThrow(() -> new BusinessException("No existe un profesor con ID " + id + "."));
    }

    public Optional<Teacher> findByDocument(String document) {
        return teacherDao.findByDocument(document);
    }

    public Teacher registerTeacher(Teacher teacher) {
        validate(teacher, null);
        teacher.setStatus(TeacherStatus.ACTIVE);
        teacher.setId(teacherDao.save(teacher));
        return teacher;
    }

    public void updateTeacher(Teacher teacher) {
        validate(teacher, teacher.getId());
        teacherDao.update(teacher);
    }

    public void changeStatus(int teacherId, TeacherStatus status) {
        Teacher teacher = getTeacher(teacherId);
        if (teacher.getStatus() == status) {
            throw new BusinessException("El profesor ya está en estado " + status + ".");
        }
        teacher.setStatus(status);
        teacherDao.update(teacher);
    }

    // ------------------------------------------------------------------ assignments

    public List<TeacherAssignment> getAssignments(int teacherId, int year) {
        return assignmentDao.findByTeacher(teacherId, year);
    }

    public TeacherAssignment getAssignment(int assignmentId) {
        return assignmentDao.findById(assignmentId).orElseThrow(() -> new BusinessException("La asignación no existe."));
    }

    /** Assigns a subject to a teacher (a teacher can teach many subjects and a subject can have many teachers by section). */
    public TeacherAssignment assignSubject(int teacherId, int subjectId, int year, String section) {
        Teacher teacher = getTeacher(teacherId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessException("Solo se pueden asignar materias a profesores activos.");
        }
        Subject subject = subjectDao.findById(subjectId).orElseThrow(() -> new BusinessException("La materia no existe."));
        if (!subject.isActive()) {
            throw new BusinessException("La materia está inactiva.");
        }
        String normalizedSection = section.toUpperCase();
        if (assignmentDao.exists(teacherId, subjectId, year, normalizedSection)) {
            throw new BusinessException("El profesor ya tiene asignada esta materia en la sección " + normalizedSection + ".");
        }
        boolean sectionTaken = assignmentDao.findBySubject(subjectId, year).stream()
                .anyMatch(a -> a.getSection().equalsIgnoreCase(normalizedSection));
        if (sectionTaken) {
            throw new BusinessException("La sección " + normalizedSection + " de " + subject.getName() + " ya tiene otro profesor asignado.");
        }
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setSubjectId(subjectId);
        assignment.setAcademicYear(year);
        assignment.setSection(normalizedSection);
        assignment.setId(assignmentDao.save(assignment));
        return assignment;
    }

    // ------------------------------------------------------------------ schedules

    public List<ClassSchedule> getTeacherSchedule(int teacherId, int year) {
        return assignmentDao.findSchedulesByTeacher(teacherId, year);
    }

    public List<ClassSchedule> getGradeSchedule(int gradeId, int year) {
        return assignmentDao.findSchedulesByGrade(gradeId, year);
    }

    /**
     * Adds a weekly time slot to an assignment, rejecting basic conflicts:
     * the teacher cannot be in two classes at once and a grade cannot have two classes at once.
     */
    public ClassSchedule addSchedule(int assignmentId, WeekDay day, LocalTime start, LocalTime end, String room) {
        requireSchedules();
        TeacherAssignment assignment = getAssignment(assignmentId);
        if (!end.isAfter(start)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (start.isBefore(SCHOOL_OPENS) || end.isAfter(SCHOOL_CLOSES)) {
            throw new BusinessException("El horario debe estar entre " + SCHOOL_OPENS + " y " + SCHOOL_CLOSES + ".");
        }
        assignmentDao.findSchedulesByTeacher(assignment.getTeacherId(), assignment.getAcademicYear()).stream()
                .filter(s -> s.overlaps(day, start, end))
                .findFirst()
                .ifPresent(s -> {
                    throw new BusinessException("Conflicto: el profesor ya dicta " + s.getSubjectName() + " (" + s.getGradeName()
                            + ") el " + s.getDay() + " de " + s.getStartTime() + " a " + s.getEndTime() + ".");
                });
        int gradeId = subjectDao.findById(assignment.getSubjectId()).map(Subject::getGradeId).orElseThrow();
        assignmentDao.findSchedulesByGrade(gradeId, assignment.getAcademicYear()).stream()
                .filter(s -> s.overlaps(day, start, end))
                .findFirst()
                .ifPresent(s -> {
                    throw new BusinessException("Conflicto: el grado " + s.getGradeName() + " ya tiene " + s.getSubjectName()
                            + " el " + s.getDay() + " de " + s.getStartTime() + " a " + s.getEndTime() + ".");
                });
        ClassSchedule schedule = new ClassSchedule();
        schedule.setAssignmentId(assignmentId);
        schedule.setDay(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setRoom(room);
        schedule.setId(assignmentDao.saveSchedule(schedule));
        return schedule;
    }

    public void removeSchedule(int scheduleId) {
        requireSchedules();
        if (!assignmentDao.deleteById(scheduleId)) {
            throw new BusinessException("No existe el horario con ID " + scheduleId + ".");
        }
    }

    /** Fails fast when the optional table horarios_clase is not installed. */
    public void requireSchedules() {
        if (!assignmentDao.schedulesAvailable()) {
            throw new BusinessException("Los horarios requieren la tabla horarios_clase (extensión 1B de docs/database.sql). "
                    + "Ejecute docs/database.sql completo para habilitarlos.");
        }
    }

    private void validate(Teacher t, Integer currentId) {
        teacherDao.findByDocument(t.getDocumentNumber())
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un profesor con el documento " + t.getDocumentNumber() + ".");
                });
        teacherDao.findByEmail(t.getEmail())
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("El correo " + t.getEmail() + " ya está registrado para otro profesor.");
                });
        if (t.getHireDate().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de contratación no puede ser futura.");
        }
    }
}
