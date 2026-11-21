package test.vista;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.DatabaseConnection;
import test.controlador.PerformanceReportController;
import test.controlador.StudentSearchController;
import test.reporte.PerformanceReportWriter;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point of the exam: runs the advanced student search directly,
 * without going through the main menu of the ERP.
 * It reuses the database connection of the main project (DatabaseConnection).
 */
public class BuscadorEstudiantesMain {

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        mostrarMenuBuscador();
    }

    private static void mostrarMenuBuscador() {
        DatabaseConnection database = DatabaseConnection.getInstance();
        try {
            database.testConnection();
        } catch (DataAccessException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Revise config/db.properties y que MySQL esté encendido.");
            return;
        }
        StudentSearchController searchController = new StudentSearchController(database);
        PerformanceReportController reportController = new PerformanceReportController(searchController, database);
        new SearchMenuView(searchController, reportController, new PerformanceReportWriter()).show();
    }
}
