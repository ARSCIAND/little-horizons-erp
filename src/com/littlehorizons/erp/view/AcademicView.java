package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.report.SubjectGradeLine;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Console screens for subjects, subject enrollment, grades and attendance. */
public class AcademicView extends ConsoleView {

    private final AcademicController controller;
    private final Lookup lookup;

    public AcademicView(InputReader input, AcademicController controller, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "GESTIÓN ACADÉMICA";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Materias"),
                new MenuOption("Listar materias por grado", this::listSubjects),
                new MenuOption("Registrar nueva materia", this::registerSubject),
                new MenuOption("Activar / desactivar materia", this::toggleSubject),
                MenuOption.section("Inscripción en materias"),
                new MenuOption("Inscribir un estudiante en las materias de su grado", this::enrollStudent),
                new MenuOption("Inscribir a todo un grado en un período", this::enrollGrade),
                MenuOption.section("Calificaciones"),
                new MenuOption("Registrar / corregir la nota de un estudiante", this::recordSingleGrade),
                new MenuOption("Registrar las notas de un curso completo", this::recordCourseGrades),
                new MenuOption("Calcular notas finales de una materia (promedio I-IV)", this::computeFinals),
                new MenuOption("Boletín del estudiante (notas y promedios)", this::reportCard),
                new MenuOption("Historial académico completo del estudiante", this::history),
                MenuOption.section("Asistencia"),
                new MenuOption("Tomar asistencia de una clase", this::takeAttendance),
                new MenuOption("Consultar asistencia de un estudiante", this::studentAttendance));
    }

    // ------------------------------------------------------------------ subjects

    private void listSubjects() {
        Grade grade = lookup.grade();
        TablePrinter.of(controller.listSubjects(grade.getId()))
                .column("ID", Subject::getId)
                .column("Código", Subject::getCode)
                .column("Materia", Subject::getName)
                .column("Horas/sem", Subject::getWeeklyHours)
                .column("Activa", s -> s.isActive() ? "Sí" : "No")
                .column("Descripción", Subject::getDescription)
                .print();
    }

    private void registerSubject() {
        Grade grade = lookup.grade();
        Subject s = new Subject();
        s.setGradeId(grade.getId());
        s.setCode(input.readMatching("  Código * (ej: ART-05): ", java.util.regex.Pattern.compile("^[A-Za-z]{2,6}-\\d{2}$"),
                "Use el formato LETRAS-NN, ej: ART-05.", false));
        s.setName(input.readText("  Nombre de la materia *: ", 3, 100));
        s.setDescription(input.readOptionalText("  Descripción: ", 500));
        s.setWeeklyHours(input.readInt("  Horas semanales * (1-10): ", 1, 10));
        Subject saved = controller.registerSubject(s);
        success("Materia " + saved.getCode() + " registrada con ID " + saved.getId() + " en " + grade.getName() + ".");
    }

    private void toggleSubject() {
        Grade grade = lookup.grade();
        List<Subject> subjects = controller.listSubjects(grade.getId());
        TablePrinter.of(subjects).column("ID", Subject::getId).column("Código", Subject::getCode)
                .column("Materia", Subject::getName).column("Activa", s -> s.isActive() ? "Sí" : "No").print();
        int id = input.readInt("  ID de la materia: ", 1, Integer.MAX_VALUE);
        Subject subject = controller.getSubject(id);
        boolean activate = !subject.isActive();
        confirmOrCancel((activate ? "¿Activar " : "¿Desactivar ") + subject.getName() + "?");
        controller.setSubjectActive(id, activate);
        success("Materia " + (activate ? "activada." : "desactivada."));
    }

    // ------------------------------------------------------------------ enrollment

    private void enrollStudent() {
        Student s = lookup.student();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(false, false);
        int added = controller.enrollStudentInGradeSubjects(s.getId(), year, period);
        success(added == 0 ? "El estudiante ya estaba inscrito en todas sus materias del período " + period + "."
                : "Se inscribió a " + s.getShortName() + " en " + added + " materia(s) del período " + period + ".");
    }

    private void enrollGrade() {
        Grade grade = lookup.grade();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(false, false);
        confirmOrCancel("¿Inscribir a todos los estudiantes activos de " + grade.getName() + " en el período " + period + "?");
        int added = controller.enrollGradeInPeriod(grade.getId(), year, period);
        success("Inscripciones nuevas creadas: " + added + ".");
    }

    // ------------------------------------------------------------------ grades

    private void recordSingleGrade() {
        Student s = lookup.student();
        Subject subject = lookup.subject();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(false, false);
        Teacher teacher = controller.getAssignedTeacher(subject.getId(), year);
        info("Profesor responsable: " + teacher.getFullName());
        BigDecimal grade = input.readGrade("  Nota (0.0 - 5.0): ");
        String notes = input.readOptionalText("  Observación: ", 500);
        boolean updated = controller.recordGrade(s.getId(), subject.getId(), teacher.getId(), year, period, grade, notes);
        success((updated ? "Nota corregida: " : "Nota registrada: ") + s.getShortName() + " - " + subject.getName()
                + " - período " + period + " = " + grade + " (" + Formats.performanceLevel(grade.doubleValue()) + ").");
    }

    private void recordCourseGrades() {
        Subject subject = lookup.subject();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(false, false);
        Teacher teacher = controller.getAssignedTeacher(subject.getId(), year);
        List<Student> roster = controller.getRoster(subject.getId(), year);
        if (roster.isEmpty()) {
            throw new BusinessException("No hay estudiantes activos en el grado de esta materia.");
        }
        Map<Integer, BigDecimal> current = controller.getGradesForPeriod(subject.getId(), year, period);
        info(subject.getName() + " | Período " + period + " | Profesor: " + teacher.getFullName());
        info("Escriba la nota de cada estudiante; Enter conserva la actual u omite.");
        int saved = 0;
        int failed = 0;
        for (Student s : roster) {
            String now = current.containsKey(s.getId()) ? current.get(s.getId()).toPlainString() : "sin nota";
            BigDecimal grade = input.readOptionalGrade(String.format("  %-38s [%s]: ", s.getFullName(), now));
            if (grade == null) {
                continue;
            }
            try {
                controller.recordGrade(s.getId(), subject.getId(), teacher.getId(), year, period, grade, null);
                saved++;
            } catch (BusinessException e) {
                failed++;
                System.out.println("    ✖ " + e.getMessage());
            }
        }
        success("Notas guardadas: " + saved + (failed > 0 ? " | con error: " + failed : "") + ".");
    }

    private void computeFinals() {
        Subject subject = lookup.subject();
        int year = lookup.year();
        confirmOrCancel("¿Calcular la nota Final de " + subject.getName() + " (" + year + ") como promedio de los períodos I a IV?");
        int[] result = controller.computeFinalGrades(subject.getId(), year);
        success("Notas finales calculadas: " + result[0] + ". Estudiantes omitidos por períodos incompletos: " + result[1] + ".");
    }

    private void reportCard() {
        Student s = lookup.student();
        int year = lookup.year();
        List<SubjectGradeLine> lines = controller.getReportCard(s.getId(), year);
        System.out.println();
        section("Boletín " + year + " - " + s.getFullName() + " (" + Formats.nullSafe(s.getCurrentGrade()) + ")");
        printReportCard(lines);
        for (AcademicPeriod period : AcademicPeriod.regularPeriods()) {
            double average = controller.getStudentAverage(s.getId(), year, period);
            if (average > 0) {
                System.out.printf("    Promedio general período %-4s %s%n", period + ":", Formats.grade(average));
            }
        }
        double yearAverage = controller.getStudentAverage(s.getId(), year, null);
        System.out.printf("    PROMEDIO GENERAL DEL AÑO:        %s (%s)%n", Formats.grade(yearAverage), Formats.performanceLevel(yearAverage));
    }

    private void printReportCard(List<SubjectGradeLine> lines) {
        TablePrinter.of(lines)
                .column("Código", SubjectGradeLine::subjectCode)
                .column("Materia", SubjectGradeLine::subjectName)
                .column("I", l -> grade(l, "I"))
                .column("II", l -> grade(l, "II"))
                .column("III", l -> grade(l, "III"))
                .column("IV", l -> grade(l, "IV"))
                .column("Promedio", l -> Formats.grade(l.average()))
                .column("Final", l -> grade(l, "Final"))
                .column("Desempeño", l -> Formats.performanceLevel(l.average()))
                .print();
    }

    private static String grade(SubjectGradeLine line, String period) {
        Double value = line.gradesByPeriod().get(period);
        return value == null ? "-" : Formats.grade(value);
    }

    private void history() {
        Student s = lookup.student();
        Map<Integer, List<SubjectGradeLine>> history = controller.getAcademicHistory(s.getId());
        if (history.isEmpty()) {
            info("El estudiante no tiene notas registradas.");
            return;
        }
        history.forEach((year, lines) -> {
            System.out.println();
            section("Año " + year);
            printReportCard(lines);
            double average = lines.stream().mapToDouble(SubjectGradeLine::average).average().orElse(0);
            System.out.printf("    Promedio del año %d: %s%n", year, Formats.grade(average));
        });
    }

    // ------------------------------------------------------------------ attendance

    private void takeAttendance() {
        Subject subject = lookup.subject();
        LocalDate date = input.readDateOrDefault("  Fecha de la clase", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura.");
        List<Student> roster = controller.getRoster(subject.getId(), date.getYear());
        if (roster.isEmpty()) {
            throw new BusinessException("No hay estudiantes activos en el grado de esta materia.");
        }
        info("Marque: P = Presente, A = Ausente, T = Tardanza, E = Excusa médica (Enter = Presente).");
        Map<Integer, AttendanceStatus> marks = new LinkedHashMap<>();
        Map<Integer, String> notes = new LinkedHashMap<>();
        for (Student s : roster) {
            AttendanceStatus status = null;
            while (status == null) {
                String code = input.readLine(String.format("  %-38s : ", s.getFullName())).toUpperCase();
                status = switch (code) {
                    case "", "P" -> AttendanceStatus.PRESENT;
                    case "A" -> AttendanceStatus.ABSENT;
                    case "T" -> AttendanceStatus.LATE;
                    case "E" -> AttendanceStatus.MEDICAL_EXCUSE;
                    default -> {
                        System.out.println("    ✖ Use P, A, T o E.");
                        yield null;
                    }
                };
            }
            marks.put(s.getId(), status);
            if (status == AttendanceStatus.MEDICAL_EXCUSE) {
                notes.put(s.getId(), input.readOptionalText("    Detalle de la excusa: ", 255));
            }
        }
        Map<AttendanceStatus, Long> summary = marks.values().stream()
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        info("Resumen: " + summary);
        confirmOrCancel("¿Guardar la asistencia del " + date + "?");
        int saved = controller.recordAttendance(subject.getId(), date, marks, notes);
        success("Asistencia guardada para " + saved + " estudiante(s).");
    }

    private void studentAttendance() {
        Student s = lookup.student();
        LocalDate to = input.readDateOrDefault("  Hasta", LocalDate.now(), d -> true, "");
        LocalDate from = input.readDateOrDefault("  Desde", to.minusDays(30), d -> !d.isAfter(to), "Debe ser anterior a la fecha final.");
        List<AttendanceRecord> records = controller.getAttendance(s.getId(), from, to);
        TablePrinter.of(records)
                .column("Fecha", r -> Formats.date(r.getDate()))
                .column("Día", r -> r.getDate().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "CO")))
                .column("Materia", AttendanceRecord::getSubjectName)
                .column("Estado", AttendanceRecord::getStatus)
                .column("Observaciones", AttendanceRecord::getNotes)
                .print();
        info("Resumen: " + controller.summarizeAttendance(records));
    }
}
