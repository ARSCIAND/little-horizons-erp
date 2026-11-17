package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.util.List;
import java.util.Optional;

/**
 * Reusable interactive pickers shared by several views (students, teachers, grades, subjects, periods).
 * Every picker accepts "0" to cancel the current operation.
 */
public class Lookup {

    private final InputReader input;
    private final StudentController students;
    private final TeacherController teachers;
    private final AcademicController academic;

    public Lookup(InputReader input, StudentController students, TeacherController teachers, AcademicController academic) {
        this.input = input;
        this.students = students;
        this.teachers = teachers;
        this.academic = academic;
    }

    /** Finds a student by ID, document number or part of the name. */
    public Student student() {
        while (true) {
            String term = input.readLine("  Estudiante (ID, documento o nombre; 0 o X = cancelar): ");
            if (term.equals("0")) {
                throw new OperationCancelledException();
            }
            if (term.isBlank()) {
                System.out.println("  ✖ Escriba un ID, documento o nombre.");
                continue;
            }
            Optional<Student> exact = exactStudent(term);
            if (exact.isPresent()) {
                System.out.println("    → " + exact.get().getFullName() + " | " + Formats.nullSafe(exact.get().getCurrentGrade())
                        + " | " + exact.get().getStatus());
                return exact.get();
            }
            List<Student> found = students.search(term);
            if (found.isEmpty()) {
                System.out.println("  ✖ No se encontraron estudiantes con '" + term + "'. Intente de nuevo.");
                continue;
            }
            if (found.size() == 1) {
                System.out.println("    → " + found.get(0).getFullName() + " | " + Formats.nullSafe(found.get(0).getCurrentGrade()));
                return found.get(0);
            }
            TablePrinter.of(found)
                    .column("ID", Student::getId)
                    .column("Documento", Student::getDocumentNumber)
                    .column("Nombre", Student::getFullName)
                    .column("Grado", Student::getCurrentGrade)
                    .column("Estado", Student::getStatus)
                    .print();
            int id = input.readInt("  ID del estudiante (0 o X = cancelar): ", 0, Integer.MAX_VALUE);
            if (id == 0) {
                throw new OperationCancelledException();
            }
            Optional<Student> chosen = found.stream().filter(s -> s.getId() == id).findFirst();
            if (chosen.isPresent()) {
                return chosen.get();
            }
            System.out.println("  ✖ El ID no está en la lista.");
        }
    }

    private Optional<Student> exactStudent(String term) {
        if (term.matches("\\d{1,6}")) {
            try {
                return Optional.of(students.getStudent(Integer.parseInt(term)));
            } catch (BusinessException ignored) {
                // not an ID: fall through to document search
            }
        }
        return students.findByDocument(term);
    }

    public Teacher teacher() {
        while (true) {
            String term = input.readLine("  Profesor (ID, documento, nombre o especialidad; 0 o X = cancelar): ");
            if (term.equals("0")) {
                throw new OperationCancelledException();
            }
            if (term.matches("\\d{1,6}")) {
                try {
                    Teacher t = teachers.getTeacher(Integer.parseInt(term));
                    System.out.println("    → " + t.getFullName() + " | " + t.getSpecialization());
                    return t;
                } catch (BusinessException ignored) {
                    // try the other criteria
                }
            }
            List<Teacher> found = term.isBlank() ? List.of() : teachers.search(term);
            if (found.isEmpty()) {
                System.out.println("  ✖ No se encontraron profesores. Intente de nuevo.");
                continue;
            }
            if (found.size() == 1) {
                System.out.println("    → " + found.get(0).getFullName() + " | " + found.get(0).getSpecialization());
                return found.get(0);
            }
            TablePrinter.of(found)
                    .column("ID", Teacher::getId)
                    .column("Nombre", Teacher::getFullName)
                    .column("Especialización", Teacher::getSpecialization)
                    .column("Estado", Teacher::getStatus)
                    .print();
            int id = input.readInt("  ID del profesor (0 o X = cancelar): ", 0, Integer.MAX_VALUE);
            if (id == 0) {
                throw new OperationCancelledException();
            }
            Optional<Teacher> chosen = found.stream().filter(t -> t.getId() == id).findFirst();
            if (chosen.isPresent()) {
                return chosen.get();
            }
            System.out.println("  ✖ El ID no está en la lista.");
        }
    }

    public Grade grade() {
        List<Grade> grades = students.getGrades();
        System.out.println("  Grados:");
        for (int i = 0; i < grades.size(); i++) {
            System.out.printf("    %2d. %s%n", i + 1, grades.get(i).getName());
        }
        int choice = input.readInt("  Seleccione el grado (0 o X = cancelar): ", 0, grades.size());
        if (choice == 0) {
            throw new OperationCancelledException();
        }
        return grades.get(choice - 1);
    }

    public Subject subject() {
        Grade grade = grade();
        return subjectOfGrade(grade.getId());
    }

    public Subject subjectOfGrade(int gradeId) {
        List<Subject> subjects = academic.listSubjects(gradeId).stream().filter(Subject::isActive).toList();
        if (subjects.isEmpty()) {
            throw new BusinessException("El grado no tiene materias activas.");
        }
        System.out.println("  Materias:");
        for (int i = 0; i < subjects.size(); i++) {
            System.out.printf("    %2d. %-10s %s%n", i + 1, subjects.get(i).getCode(), subjects.get(i).getName());
        }
        int choice = input.readInt("  Seleccione la materia (0 o X = cancelar): ", 0, subjects.size());
        if (choice == 0) {
            throw new OperationCancelledException();
        }
        return subjects.get(choice - 1);
    }

    /** Period picker; when {@code allowAll} is true, option 5 means "whole year". */
    public AcademicPeriod period(boolean allowAll, boolean includeFinal) {
        AcademicPeriod[] periods = includeFinal ? AcademicPeriod.values() : AcademicPeriod.regularPeriods();
        System.out.println("  Período académico:");
        for (int i = 0; i < periods.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, periods[i].getDbValue());
        }
        if (allowAll) {
            System.out.printf("    %d. Año completo (períodos I-IV)%n", periods.length + 1);
        }
        int choice = input.readInt("  Seleccione: ", 1, periods.length + (allowAll ? 1 : 0));
        return choice > periods.length ? null : periods[choice - 1];
    }

    public int year() {
        int current = academic.currentYear();
        return input.readOptionalInt("  Año académico [Enter = " + current + "]: ", 2000, 2100).orElse(current);
    }
}
