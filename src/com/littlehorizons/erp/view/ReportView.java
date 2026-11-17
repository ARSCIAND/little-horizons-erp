package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.ParticipationSummary;
import com.littlehorizons.erp.model.report.StudentAverage;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.model.report.SupportCase;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Cross-module statistics and reports. */
public class ReportView extends ConsoleView {

    private final ReportController reports;
    private final FinanceController finance;
    private final ResourceController resources;
    private final Lookup lookup;

    public ReportView(InputReader input, ReportController reports, FinanceController finance, ResourceController resources, Lookup lookup) {
        super(input);
        this.reports = reports;
        this.finance = finance;
        this.resources = resources;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "REPORTES Y ESTADÍSTICAS";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Académicos"),
                new MenuOption("Promedio del curso en cada materia", this::courseAverages),
                new MenuOption("Ranking de estudiantes de un grado", this::ranking),
                new MenuOption("Estudiantes que necesitan apoyo (notas < 3.0)", this::support),
                new MenuOption("Patrones de inasistencia (> 3 ausencias en una semana)", this::absences),
                MenuOption.section("Institucionales"),
                new MenuOption("Participación extracurricular por estudiante", this::participation),
                new MenuOption("Panel general del colegio", this::dashboard));
    }

    private void courseAverages() {
        Grade grade = lookup.grade();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(true, true);
        section("Promedios de " + grade.getName() + " - " + (period == null ? "año " + year : "período " + period + " " + year));
        List<SubjectAverage> rows = reports.courseAverages(grade.getId(), year, period);
        TablePrinter.of(rows)
                .column("Código", SubjectAverage::subjectCode)
                .column("Materia", SubjectAverage::subjectName)
                .column("Promedio", r -> Formats.grade(r.average()))
                .column("Mínima", r -> Formats.grade(r.min()))
                .column("Máxima", r -> Formats.grade(r.max()))
                .column("Estudiantes", SubjectAverage::students)
                .column("Desempeño", r -> Formats.performanceLevel(r.average()))
                .print();
        rows.stream().mapToDouble(SubjectAverage::average).average()
                .ifPresent(avg -> info("Promedio general del curso: " + Formats.grade(avg)));
    }

    private void ranking() {
        Grade grade = lookup.grade();
        int year = lookup.year();
        AcademicPeriod period = lookup.period(true, false);
        List<StudentAverage> rows = reports.ranking(grade.getId(), year, period);
        int[] position = {0};
        TablePrinter.of(rows)
                .column("Puesto", r -> ++position[0])
                .column("Estudiante", StudentAverage::studentName)
                .column("Promedio", r -> Formats.grade(r.average()))
                .column("Materias", StudentAverage::subjects)
                .column("Desempeño", r -> Formats.performanceLevel(r.average()))
                .print();
    }

    private void support() {
        int year = lookup.year();
        AcademicPeriod period = lookup.period(true, false);
        List<SupportCase> cases = reports.studentsNeedingSupport(year, period);
        TablePrinter.of(cases)
                .column("ID", SupportCase::studentId)
                .column("Estudiante", SupportCase::studentName)
                .column("Grado", SupportCase::gradeName)
                .column("Promedio", c -> Formats.grade(c.average()))
                .column("Materias en riesgo", c -> String.join(", ", c.failingSubjects()))
                .print();
        info("Se consideran en riesgo las materias con promedio inferior a " + ReportController.PASSING_GRADE + ".");
    }

    private void absences() {
        LocalDate to = input.readDateOrDefault("  Hasta", LocalDate.now(), d -> true, "");
        LocalDate from = input.readDateOrDefault("  Desde", to.minusWeeks(4), d -> !d.isAfter(to), "Debe ser anterior a la fecha final.");
        List<AbsenceAlert> alerts = reports.absencePatterns(from, to);
        TablePrinter.of(alerts)
                .column("ID", AbsenceAlert::studentId)
                .column("Estudiante", AbsenceAlert::studentName)
                .column("Grado", AbsenceAlert::gradeName)
                .column("Semana del", a -> Formats.date(a.weekStart()))
                .column("Ausencias", AbsenceAlert::absences)
                .print();
        info("Criterio: más de " + ReportController.WEEKLY_ABSENCE_THRESHOLD + " ausencias sin excusa en la misma semana (lunes a sábado).");
    }

    private void participation() {
        List<ParticipationSummary> rows = reports.participation();
        TablePrinter.of(rows)
                .column("Estudiante", ParticipationSummary::studentName)
                .column("Grado", ParticipationSummary::gradeName)
                .column("Clubes activos", ParticipationSummary::activeClubs)
                .column("Eventos", ParticipationSummary::events)
                .column("Podios", ParticipationSummary::podiums)
                .column("Puntaje", ParticipationSummary::score)
                .print();
        long without = rows.stream().filter(r -> r.activeClubs() == 0 && r.events() == 0).count();
        info("Estudiantes activos sin ninguna actividad extracurricular: " + without + ".");
    }

    private void dashboard() {
        int year = finance.currentYear();
        LocalDate today = LocalDate.now();
        section("Estudiantes por estado");
        reports.studentsByStatus().forEach((status, count) -> field(status, count));
        System.out.println();
        section("Estudiantes activos por grado");
        Map<String, Long> byGrade = reports.activeStudentsByGrade();
        byGrade.forEach((grade, count) -> field(grade, count + " " + "█".repeat(count.intValue())));
        System.out.println();
        section("Finanzas " + year);
        BigDecimal income = finance.incomeByConcept(year).stream().map(r -> r.total()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var delinquents = finance.delinquentAccounts(year, today);
        field("Recaudo del año", Formats.money(income));
        field("Estudiantes en mora", delinquents.size());
        field("Cartera vencida", Formats.money(delinquents.stream().map(d -> d.amountPending()).reduce(BigDecimal.ZERO, BigDecimal::add)));
        System.out.println();
        section("Recursos");
        field("Préstamos abiertos", resources.openLoans().size());
        field("Préstamos vencidos", resources.overdueLoans(today).size());
        System.out.println();
        section("Alertas académicas " + year);
        field("Estudiantes que requieren apoyo", reports.studentsNeedingSupport(year, null).size());
        field("Alertas de inasistencia (4 sem.)", reports.absencePatterns(today.minusWeeks(4), today).size());
    }
}
