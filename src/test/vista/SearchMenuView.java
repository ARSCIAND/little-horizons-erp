package test.vista;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.ErrorLogger;
import test.controlador.PerformanceReportController;
import test.controlador.StudentSearchController;
import test.modelo.GradeOption;
import test.modelo.PerformanceReport;
import test.modelo.StudentSearchResult;
import test.reporte.PerformanceReportWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Console view of the advanced student search: main menu, grade selection, results
 * and the option that generates the performance report as a .txt file.
 * It only reads input and prints; all the logic lives in the controllers.
 */
public class SearchMenuView {

    private static final String DOUBLE_LINE = "=".repeat(79);
    private static final String SINGLE_LINE = "-".repeat(79);

    private final StudentSearchController searchController;
    private final PerformanceReportController reportController;
    private final PerformanceReportWriter reportWriter;
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public SearchMenuView(StudentSearchController searchController, PerformanceReportController reportController,
                          PerformanceReportWriter reportWriter) {
        this.searchController = searchController;
        this.reportController = reportController;
        this.reportWriter = reportWriter;
    }

    /** Main loop of the search menu. Returns when the user chooses 0. */
    public void show() {
        while (true) {
            printMainMenu();
            String option = readLine("Seleccione una opción: ");
            if (option == null) {
                return; // end of input
            }
            try {
                switch (option.trim()) {
                    case "1" -> filterByGrade();
                    case "2", "3", "4", "5" -> showNotImplemented();
                    case "6" -> generatePerformanceReport();
                    case "0" -> {
                        System.out.println();
                        System.out.println("Saliendo del buscador...");
                        System.out.println("Gracias por usar el sistema.");
                        return;
                    }
                    default -> {
                        System.out.println();
                        System.out.println("Error: Debe seleccionar una opción entre 0 y 6.");
                        pause("Presione Enter para intentar nuevamente...");
                    }
                }
            } catch (DataAccessException e) {
                System.out.println();
                System.out.println("Error: " + e.getMessage());
                pause("Presione Enter para volver al menú principal...");
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(DOUBLE_LINE);
        System.out.println("            BUSCADOR AVANZADO DE ESTUDIANTES - PROTOTIPO");
        System.out.println(DOUBLE_LINE);
        System.out.println();
        System.out.println("Seleccione el filtro que desea aplicar:");
        System.out.println();
        System.out.println("1. Filtrar por grado académico [FUNCIONAL]");
        System.out.println("2. Filtrar por rango de edad [NO IMPLEMENTADO]");
        System.out.println("3. Filtrar por género [NO IMPLEMENTADO]");
        System.out.println("4. Filtrar por estado de matrícula [NO IMPLEMENTADO]");
        System.out.println("5. Filtrar por rango de promedio académico [NO IMPLEMENTADO]");
        System.out.println("6. Generar reporte de rendimiento académico por grado (.txt) [FUNCIONAL]");
        System.out.println("0. Salir");
        System.out.println();
    }

    private void showNotImplemented() {
        System.out.println();
        System.out.println("Esta funcionalidad aún no ha sido implementada.");
        System.out.println("Será agregada en futuras versiones del sistema.");
        System.out.println();
        pause("Presione Enter para continuar...");
    }

    // ------------------------------------------------------------------ option 1: filter by grade

    private void filterByGrade() {
        Optional<GradeOption> selected = selectGrade("--- FILTRAR POR GRADO ACADÉMICO ---",
                "Ingrese el número del grado que desea filtrar (o 0 para cancelar): ");
        if (selected.isEmpty()) {
            return;
        }
        GradeOption grade = selected.get();
        System.out.println();
        System.out.println("Procesando búsqueda...");
        System.out.println("Consultando estudiantes matriculados en " + grade.name() + "...");

        List<StudentSearchResult> results = searchController.searchByGrade(grade);
        printResults(grade, results);
    }

    private void printResults(GradeOption grade, List<StudentSearchResult> results) {
        int year = StudentSearchController.ACADEMIC_YEAR;
        System.out.println();
        System.out.println(DOUBLE_LINE);
        System.out.println("            RESULTADOS DE LA BÚSQUEDA");
        System.out.println(DOUBLE_LINE);
        System.out.println();
        System.out.println("Filtro aplicado: Grado académico = " + grade.name());
        System.out.println();

        if (results.isEmpty()) {
            System.out.println("No se encontraron estudiantes matriculados en " + grade.name() + " para el año " + year + ".");
            System.out.println();
            System.out.println("Posibles razones:");
            System.out.println("  - No hay estudiantes matriculados en ese grado para el año " + year);
            System.out.println("  - Verifique que la base de datos tenga información del año actual");
        } else {
            System.out.println(results.size() == 1
                    ? "Se encontró 1 estudiante matriculado en " + grade.name() + " (año " + year + "):"
                    : "Se encontraron " + results.size() + " estudiantes matriculados en " + grade.name() + " (año " + year + "):");
            System.out.println();
            System.out.println(SINGLE_LINE);
            for (StudentSearchResult student : results) {
                System.out.println("ID: " + student.studentId());
                System.out.println("Nombre: " + student.fullName());
                System.out.println("Documento: " + student.document());
                System.out.println(SINGLE_LINE);
            }
            System.out.println();
            System.out.println("Total de resultados: " + results.size() + (results.size() == 1 ? " estudiante" : " estudiantes"));
        }
        System.out.println();
        System.out.println(DOUBLE_LINE);
        pause("Presione Enter para volver al menú principal...");
        System.out.println(DOUBLE_LINE);
    }

    // ------------------------------------------------------------------ option 6: performance report

    private void generatePerformanceReport() {
        Optional<GradeOption> selected = selectGrade("--- REPORTE DE RENDIMIENTO ACADÉMICO POR GRADO (.txt) ---",
                "Ingrese el número del grado del reporte (o 0 para cancelar): ");
        if (selected.isEmpty()) {
            return;
        }
        GradeOption grade = selected.get();
        System.out.println();
        System.out.println("Calculando promedios de " + grade.name() + " (año " + StudentSearchController.ACADEMIC_YEAR + ")...");

        PerformanceReport report = reportController.build(grade);
        if (report.isEmpty()) {
            System.out.println();
            System.out.println("No se generó el reporte: no hay estudiantes con matrícula activa y notas en "
                    + grade.name() + " para el año " + StudentSearchController.ACADEMIC_YEAR + ".");
            System.out.println();
            pause("Presione Enter para volver al menú principal...");
            return;
        }
        try {
            Path file = reportWriter.write(report);
            System.out.println();
            System.out.print(reportWriter.render(report));
            System.out.println();
            System.out.println("Reporte generado correctamente.");
            System.out.println("Archivo: " + file);
        } catch (IOException e) {
            ErrorLogger.getInstance().error("SearchMenuView", "Could not write the performance report", e);
            System.out.println();
            System.out.println("Error: no se pudo escribir el archivo del reporte. El detalle quedó en error_log.txt.");
        }
        System.out.println();
        pause("Presione Enter para volver al menú principal...");
    }

    // ------------------------------------------------------------------ shared grade selector

    /**
     * Shows the grades and validates the choice: out of range, non numeric and 0 (cancel).
     * Returns empty when the user cancels.
     */
    private Optional<GradeOption> selectGrade(String title, String prompt) {
        List<GradeOption> grades = searchController.getGrades();
        while (true) {
            System.out.println();
            System.out.println(title);
            System.out.println();
            System.out.println("Grados disponibles en el sistema:");
            for (int i = 0; i < grades.size(); i++) {
                System.out.println((i + 1) + ". " + grades.get(i).name());
            }
            System.out.println();
            String text = readLine(prompt);
            if (text == null) {
                return Optional.empty();
            }
            int number;
            try {
                number = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                System.out.println();
                System.out.println("Error: Debe ingresar un número válido.");
                pause("Presione Enter para intentar nuevamente...");
                continue;
            }
            if (number == 0) {
                System.out.println();
                System.out.println("Operación cancelada.");
                pause("Presione Enter para volver al menú principal...");
                return Optional.empty();
            }
            if (number < 1 || number > grades.size()) {
                System.out.println();
                System.out.println("Error: Debe seleccionar un número entre 1 y " + grades.size() + ".");
                pause("Presione Enter para intentar nuevamente...");
                continue;
            }
            return Optional.of(grades.get(number - 1));
        }
    }

    // ------------------------------------------------------------------ input helpers

    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void pause(String message) {
        readLine(message);
    }
}
