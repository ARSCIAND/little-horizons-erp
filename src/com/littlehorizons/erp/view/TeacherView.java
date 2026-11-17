package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.enums.WeekDay;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Console screens for module 1.2: teachers, subject assignments and class schedules. */
public class TeacherView extends ConsoleView {

    private static final DocumentType[] ADULT_DOCUMENTS = {DocumentType.CITIZENSHIP_CARD, DocumentType.FOREIGNER_ID, DocumentType.PASSPORT};

    private final TeacherController controller;
    private final Lookup lookup;

    public TeacherView(InputReader input, TeacherController controller, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "GESTIÓN DE PROFESORES";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Directorio"),
                new MenuOption("Registrar profesor", this::register),
                new MenuOption("Listar profesores", this::list),
                new MenuOption("Ver ficha del profesor (carga académica y horario)", this::profile),
                new MenuOption("Actualizar datos del profesor", this::update),
                new MenuOption("Cambiar estado (activo, inactivo, licencia)", this::changeStatus),
                MenuOption.section("Carga académica y horarios"),
                new MenuOption("Asignar materia a un profesor", this::assignSubject),
                new MenuOption("Agregar franja horaria a una asignación", this::addSchedule),
                new MenuOption("Eliminar franja horaria", this::removeSchedule),
                new MenuOption("Ver horario semanal de un grado", this::gradeSchedule));
    }

    private void register() {
        Teacher t = new Teacher();
        t.setDocumentType(input.readEnum("Tipo de documento *", ADULT_DOCUMENTS));
        t.setDocumentNumber(input.readDocument("  Número de documento *: "));
        t.setFirstName(input.readName("  Primer nombre *: "));
        t.setMiddleName(input.readOptionalName("  Segundo nombre: "));
        t.setLastName(input.readName("  Primer apellido *: "));
        t.setSecondLastName(input.readOptionalName("  Segundo apellido: "));
        t.setEmail(input.readEmail("  Correo institucional *: ", false));
        t.setPhone(input.readPhone("  Teléfono: ", true));
        t.setSpecialization(input.readText("  Especialización * (ej: Matemáticas): ", 3, 100));
        t.setHireDate(input.readDateOrDefault("  Fecha de contratación", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura."));
        confirmOrCancel("¿Registrar a " + t.getFullName() + "?");
        Teacher saved = controller.registerTeacher(t);
        success("Profesor registrado con ID " + saved.getId() + ".");
    }

    private void list() {
        System.out.println("  Filtrar: 1. Todos  2. Activos  3. Inactivos  4. En licencia");
        int option = input.readInt("  Seleccione: ", 1, 4);
        TeacherStatus status = option == 1 ? null : TeacherStatus.values()[option - 2];
        TablePrinter.of(controller.listTeachers(status))
                .column("ID", Teacher::getId)
                .column("Documento", Teacher::getDocumentLabel)
                .column("Nombre", Teacher::getFullName)
                .column("Especialización", Teacher::getSpecialization)
                .column("Correo", Teacher::getEmail)
                .column("Estado", Teacher::getStatus)
                .print();
    }

    private void profile() {
        Teacher t = lookup.teacher();
        int year = lookup.year();
        section("Datos del profesor");
        field("Nombre", t.getFullName());
        field("Documento", t.getDocumentLabel());
        field("Especialización", t.getSpecialization());
        field("Correo / teléfono", t.getEmail() + " / " + Formats.nullSafe(t.getPhone()));
        field("Contratado desde", Formats.date(t.getHireDate()));
        field("Estado", t.getStatus());
        System.out.println();
        section("Carga académica " + year);
        List<TeacherAssignment> assignments = controller.getAssignments(t.getId(), year);
        printAssignments(assignments);
        System.out.println();
        section("Horario semanal " + year);
        printSchedule(controller.getTeacherSchedule(t.getId(), year));
    }

    private void update() {
        Teacher t = controller.getTeacher(lookup.teacher().getId());
        info("Presione Enter para conservar el valor actual.");
        t.setFirstName(keep("Primer nombre", t.getFirstName(), 100));
        t.setLastName(keep("Primer apellido", t.getLastName(), 100));
        String email = input.readEmail("  Correo [" + t.getEmail() + "]: ", true);
        if (email != null) t.setEmail(email);
        String phone = input.readPhone("  Teléfono [" + Formats.nullSafe(t.getPhone()) + "]: ", true);
        if (phone != null) t.setPhone(phone);
        t.setSpecialization(keep("Especialización", t.getSpecialization(), 100));
        confirmOrCancel("¿Guardar los cambios?");
        controller.updateTeacher(t);
        success("Profesor actualizado.");
    }

    private void changeStatus() {
        Teacher t = lookup.teacher();
        info("Estado actual: " + t.getStatus());
        TeacherStatus status = input.readEnum("Nuevo estado", TeacherStatus.values());
        controller.changeStatus(t.getId(), status);
        success("Estado actualizado a " + status + ".");
    }

    private void assignSubject() {
        Teacher t = lookup.teacher();
        Subject subject = lookup.subject();
        int year = lookup.year();
        String section = input.readMatching("  Sección [A, B, C...]: ", java.util.regex.Pattern.compile("^[A-Za-z]$"), "Use una sola letra.", false);
        TeacherAssignment a = controller.assignSubject(t.getId(), subject.getId(), year, section);
        success("Asignación creada (ID " + a.getId() + "): " + t.getShortName() + " dicta " + subject.getName() + " sección " + a.getSection() + ".");
    }

    private void addSchedule() {
        controller.requireSchedules();
        Teacher t = lookup.teacher();
        int year = lookup.year();
        List<TeacherAssignment> assignments = controller.getAssignments(t.getId(), year);
        if (assignments.isEmpty()) {
            throw new BusinessException("El profesor no tiene materias asignadas en " + year + ".");
        }
        printAssignments(assignments);
        int id = input.readInt("  ID de la asignación: ", 1, Integer.MAX_VALUE);
        if (assignments.stream().noneMatch(a -> a.getId() == id)) {
            throw new BusinessException("La asignación no pertenece al profesor.");
        }
        WeekDay day = input.readEnum("Día", WeekDay.values());
        LocalTime start = input.readTime("  Hora de inicio (HH:MM): ");
        LocalTime end = input.readTime("  Hora de fin (HH:MM): ");
        String room = input.readOptionalText("  Aula: ", 50);
        ClassSchedule s = controller.addSchedule(id, day, start, end, room);
        success("Franja registrada (ID " + s.getId() + "): " + day + " " + start + "-" + end + ". Sin conflictos.");
    }

    private void removeSchedule() {
        controller.requireSchedules();
        Teacher t = lookup.teacher();
        int year = lookup.year();
        List<ClassSchedule> schedule = controller.getTeacherSchedule(t.getId(), year);
        printSchedule(schedule);
        if (schedule.isEmpty()) {
            return;
        }
        int id = input.readInt("  ID de la franja a eliminar: ", 1, Integer.MAX_VALUE);
        if (schedule.stream().noneMatch(s -> s.getId() == id)) {
            throw new BusinessException("La franja no pertenece al profesor.");
        }
        confirmOrCancel("¿Eliminar la franja " + id + "?");
        controller.removeSchedule(id);
        success("Franja eliminada.");
    }

    private void gradeSchedule() {
        Grade grade = lookup.grade();
        int year = lookup.year();
        section("Horario de " + grade.getName() + " - " + year);
        printSchedule(controller.getGradeSchedule(grade.getId(), year));
    }

    private void printAssignments(List<TeacherAssignment> assignments) {
        TablePrinter.of(assignments)
                .column("ID", TeacherAssignment::getId)
                .column("Grado", TeacherAssignment::getGradeName)
                .column("Materia", TeacherAssignment::getSubjectName)
                .column("Sección", TeacherAssignment::getSection)
                .print();
    }

    private void printSchedule(List<ClassSchedule> schedule) {
        TablePrinter.of(schedule)
                .column("ID", ClassSchedule::getId)
                .column("Día", ClassSchedule::getDay)
                .column("Inicio", ClassSchedule::getStartTime)
                .column("Fin", ClassSchedule::getEndTime)
                .column("Grado", ClassSchedule::getGradeName)
                .column("Materia", ClassSchedule::getSubjectName)
                .column("Profesor", ClassSchedule::getTeacherName)
                .column("Aula", ClassSchedule::getRoom)
                .print();
    }
}
