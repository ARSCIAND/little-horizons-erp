package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.StudentProfile;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Console screens for module 1.1: students, guardians and enrollment. */
public class StudentView extends ConsoleView {

    private static final Pattern BLOOD_TYPE = Pattern.compile("^(A|B|AB|O)[+-]$");
    private static final DocumentType[] GUARDIAN_DOCUMENTS = {DocumentType.CITIZENSHIP_CARD, DocumentType.FOREIGNER_ID, DocumentType.PASSPORT};

    private final StudentController controller;
    private final Lookup lookup;

    public StudentView(InputReader input, StudentController controller, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "GESTIÓN DE ESTUDIANTES";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Estudiantes"),
                new MenuOption("Registrar nuevo estudiante (con matrícula y acudiente)", this::registerStudent),
                new MenuOption("Listar estudiantes", this::listStudents),
                new MenuOption("Buscar estudiante", this::searchStudents),
                new MenuOption("Ver ficha completa del estudiante", this::showProfile),
                new MenuOption("Actualizar datos del estudiante", this::updateStudent),
                new MenuOption("Cambiar estado (activo, retirado, graduado, suspendido)", this::changeStatus),
                new MenuOption("Matricular estudiante en un año académico", this::enrollInYear),
                new MenuOption("Eliminar estudiante registrado por error", this::deleteStudent),
                MenuOption.section("Acudientes"),
                new MenuOption("Registrar acudiente", this::registerGuardianOnly),
                new MenuOption("Vincular acudiente a un estudiante", this::linkGuardian),
                new MenuOption("Desvincular acudiente de un estudiante", this::unlinkGuardian),
                new MenuOption("Listar acudientes y estudiantes a cargo", this::listGuardians));
    }

    // ------------------------------------------------------------------ students

    private void registerStudent() {
        info("Complete los datos. Los campos marcados con * son obligatorios.");
        Student s = new Student();
        s.setDocumentType(input.readEnum("Tipo de documento *", DocumentType.values()));
        s.setDocumentNumber(input.readDocument("  Número de documento *: "));
        s.setFirstName(input.readName("  Primer nombre *: "));
        s.setMiddleName(input.readOptionalName("  Segundo nombre: "));
        s.setLastName(input.readName("  Primer apellido *: "));
        s.setSecondLastName(input.readOptionalName("  Segundo apellido: "));
        s.setBirthDate(input.readDate("  Fecha de nacimiento * (AAAA-MM-DD): ", d -> d.isBefore(LocalDate.now()), "La fecha debe ser pasada."));
        s.setGender(input.readEnum("Género *", Gender.values()));
        s.setAddress(input.readOptionalText("  Dirección: ", 255));
        s.setPhone(input.readPhone("  Teléfono: ", true));
        s.setEmail(input.readEmail("  Correo electrónico: ", true));
        String blood = input.readMatching("  Tipo de sangre (ej: O+): ", BLOOD_TYPE, "Use A+, A-, B+, B-, AB+, AB-, O+ u O-.", true);
        s.setBloodType(blood == null ? null : blood.toUpperCase());
        s.setAllergies(input.readOptionalText("  Alergias: ", 500));
        s.setMedicalConditions(input.readOptionalText("  Condiciones médicas: ", 500));
        s.setAdmissionDate(input.readDateOrDefault("  Fecha de ingreso", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura."));

        System.out.println("\n  Matrícula " + controller.currentYear() + ":");
        Grade grade = lookup.grade();

        System.out.println("\n  Acudiente responsable (obligatorio):");
        String document = input.readDocument("  Documento del acudiente *: ");
        Optional<Guardian> existing = controller.findGuardianByDocument(document);
        Guardian newGuardian = null;
        Integer existingId = null;
        if (existing.isPresent()) {
            info("Acudiente ya registrado: " + existing.get().getFullName() + " (" + existing.get().getRelationship() + "). Se vinculará.");
            existingId = existing.get().getId();
        } else {
            info("Acudiente nuevo, complete sus datos:");
            newGuardian = readGuardian(document);
        }

        System.out.println();
        section("Resumen");
        field("Estudiante", s.getFullName() + " (" + s.getDocumentLabel() + ")");
        field("Grado " + controller.currentYear(), grade.getName());
        field("Acudiente", existing.map(Guardian::getFullName).orElse(newGuardian == null ? "-" : newGuardian.getFullName()));
        confirmOrCancel("¿Confirmar el registro?");
        Student saved = controller.registerStudent(s, grade.getId(), newGuardian, existingId);
        success("Estudiante registrado con ID " + saved.getId() + " y matriculado en " + grade.getName() + ".");
    }

    private Guardian readGuardian(String document) {
        Guardian g = new Guardian();
        g.setDocumentType(input.readEnum("Tipo de documento del acudiente *", GUARDIAN_DOCUMENTS));
        g.setDocumentNumber(document);
        g.setFirstName(input.readName("  Primer nombre *: "));
        g.setMiddleName(input.readOptionalName("  Segundo nombre: "));
        g.setLastName(input.readName("  Primer apellido *: "));
        g.setSecondLastName(input.readOptionalName("  Segundo apellido: "));
        g.setRelationship(input.readName("  Parentesco * (Padre, Madre, Abuelo, Tutor Legal...): "));
        g.setPhone(input.readPhone("  Teléfono principal *: ", false));
        g.setSecondaryPhone(input.readPhone("  Teléfono secundario: ", true));
        g.setEmail(input.readEmail("  Correo electrónico: ", true));
        g.setAddress(input.readOptionalText("  Dirección: ", 255));
        g.setOccupation(input.readOptionalText("  Ocupación: ", 100));
        g.setFinanciallyResponsible(input.confirm("  ¿Es responsable de los pagos?"));
        return g;
    }

    private void listStudents() {
        System.out.println("  Filtrar por estado: 1. Todos  2. Activo  3. Graduado  4. Retirado  5. Suspendido");
        int option = input.readInt("  Seleccione: ", 1, 5);
        StudentStatus status = option == 1 ? null : StudentStatus.values()[option - 2];
        printStudents(controller.listStudents(status));
    }

    private void searchStudents() {
        String term = input.readText("  Nombre, apellido o documento: ", 1, 100);
        printStudents(controller.search(term));
    }

    private void printStudents(List<Student> students) {
        TablePrinter.of(students)
                .column("ID", Student::getId)
                .column("Documento", Student::getDocumentLabel)
                .column("Nombre completo", Student::getFullName)
                .column("Edad", Student::getAge)
                .column("Grado actual", Student::getCurrentGrade)
                .column("Estado", Student::getStatus)
                .print();
    }

    private void showProfile() {
        Student selected = lookup.student();
        StudentProfile profile = controller.getProfile(selected.getId());
        Student s = profile.student();
        System.out.println();
        section("Datos personales");
        field("ID", s.getId());
        field("Nombre", s.getFullName());
        field("Documento", s.getDocumentLabel());
        field("Nacimiento", Formats.date(s.getBirthDate()) + " (" + s.getAge() + " años)");
        field("Género", s.getGender());
        field("Dirección", s.getAddress());
        field("Teléfono / correo", Formats.nullSafe(s.getPhone()) + " / " + Formats.nullSafe(s.getEmail()));
        field("Tipo de sangre", s.getBloodType());
        field("Alergias", s.getAllergies());
        field("Condiciones médicas", s.getMedicalConditions());
        field("Fecha de ingreso", Formats.date(s.getAdmissionDate()));
        field("Estado", s.getStatus());
        System.out.println();
        section("Acudientes / contactos de emergencia");
        TablePrinter.of(profile.guardians())
                .column("ID", Guardian::getId)
                .column("Nombre", Guardian::getFullName)
                .column("Parentesco", Guardian::getRelationship)
                .column("Teléfono", Guardian::getPhone)
                .column("Principal", g -> g.isPrimaryContact() ? "Sí" : "No")
                .column("Resp. pagos", g -> g.isFinanciallyResponsible() ? "Sí" : "No")
                .print();
        System.out.println();
        section("Historial de matrículas");
        TablePrinter.of(profile.enrollments())
                .column("Año", Enrollment::getAcademicYear)
                .column("Grado", Enrollment::getGradeName)
                .column("Estado", Enrollment::getStatus)
                .column("Fecha matrícula", e -> Formats.date(e.getEnrollmentDate()))
                .column("Fecha retiro", e -> Formats.date(e.getWithdrawalDate()))
                .column("Observaciones", Enrollment::getNotes)
                .print();
    }

    private void updateStudent() {
        Student s = controller.getStudent(lookup.student().getId());
        info("Presione Enter para conservar el valor actual.");
        s.setFirstName(keep("Primer nombre", s.getFirstName(), 100));
        s.setMiddleName(keep("Segundo nombre", s.getMiddleName(), 100));
        s.setLastName(keep("Primer apellido", s.getLastName(), 100));
        s.setSecondLastName(keep("Segundo apellido", s.getSecondLastName(), 100));
        s.setAddress(keep("Dirección", s.getAddress(), 255));
        String phone = input.readPhone("  Teléfono [" + Formats.nullSafe(s.getPhone()) + "]: ", true);
        if (phone != null) s.setPhone(phone);
        String email = input.readEmail("  Correo [" + Formats.nullSafe(s.getEmail()) + "]: ", true);
        if (email != null) s.setEmail(email);
        s.setAllergies(keep("Alergias", s.getAllergies(), 500));
        s.setMedicalConditions(keep("Condiciones médicas", s.getMedicalConditions(), 500));
        confirmOrCancel("¿Guardar los cambios?");
        controller.updateStudent(s);
        success("Datos de " + s.getFullName() + " actualizados.");
    }

    private void changeStatus() {
        Student s = lookup.student();
        info("Estado actual: " + s.getStatus());
        StudentStatus status = input.readEnum("Nuevo estado", StudentStatus.values());
        String reason = input.readOptionalText("  Motivo / observación: ", 255);
        confirmOrCancel("¿Cambiar el estado de " + s.getShortName() + " a " + status + "?");
        controller.changeStatus(s.getId(), status, reason);
        success("Estado actualizado a " + status + ".");
    }

    private void enrollInYear() {
        Student s = lookup.student();
        int year = lookup.year();
        Grade grade = lookup.grade();
        confirmOrCancel("¿Matricular a " + s.getShortName() + " en " + grade.getName() + " para " + year + "?");
        controller.enrollInYear(s.getId(), grade.getId(), year);
        success("Matrícula " + year + " registrada en " + grade.getName() + ".");
    }

    private void deleteStudent() {
        Student s = lookup.student();
        confirmOrCancel("¿Eliminar definitivamente a " + s.getFullName() + "? Esta acción no se puede deshacer");
        controller.deleteStudent(s.getId());
        success("Estudiante eliminado.");
    }

    // ------------------------------------------------------------------ guardians

    private void registerGuardianOnly() {
        String document = input.readDocument("  Documento del acudiente *: ");
        if (controller.findGuardianByDocument(document).isPresent()) {
            error("Ya existe un acudiente con ese documento.");
            return;
        }
        Guardian g = controller.registerGuardian(readGuardian(document));
        success("Acudiente registrado con ID " + g.getId() + ". Ahora puede vincularlo a sus estudiantes.");
    }

    private void linkGuardian() {
        Student s = lookup.student();
        String document = input.readDocument("  Documento del acudiente: ");
        Guardian g = controller.findGuardianByDocument(document)
                .orElseThrow(() -> new BusinessException(
                        "No existe un acudiente con ese documento. Regístrelo primero."));
        boolean primary = input.confirm("  ¿Será el acudiente principal?");
        boolean emergency = input.confirm("  ¿Es contacto de emergencia?");
        controller.linkGuardian(s.getId(), g.getId(), primary, emergency);
        success(g.getFullName() + " vinculado a " + s.getFullName() + ".");
    }

    private void unlinkGuardian() {
        Student s = lookup.student();
        List<Guardian> guardians = controller.getGuardiansOf(s.getId());
        TablePrinter.of(guardians)
                .column("ID", Guardian::getId)
                .column("Nombre", Guardian::getFullName)
                .column("Parentesco", Guardian::getRelationship)
                .print();
        int id = input.readInt("  ID del acudiente a desvincular (0 = cancelar): ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            throw new OperationCancelledException();
        }
        controller.unlinkGuardian(s.getId(), id);
        success("Acudiente desvinculado.");
    }

    private void listGuardians() {
        Map<Guardian, List<Student>> data = controller.guardiansWithStudents();
        List<Map.Entry<Guardian, List<Student>>> rows = List.copyOf(data.entrySet());
        TablePrinter.of(rows)
                .column("ID", e -> e.getKey().getId())
                .column("Acudiente", e -> e.getKey().getFullName())
                .column("Parentesco", e -> e.getKey().getRelationship())
                .column("Teléfono", e -> e.getKey().getPhone())
                .column("N° est.", e -> e.getValue().size())
                .column("Estudiantes a cargo", e -> e.getValue().stream().map(Student::getShortName).collect(Collectors.joining(", ")))
                .print();
    }
}
