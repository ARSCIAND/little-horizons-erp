package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;

/** Main menu: groups the options by module (requirement 5.2) and delegates to each module view. */
public class MainMenuView {

    private final InputReader input;
    private final List<ConsoleView> modules;

    /**
     * @param modules module views in the order they appear in the menu (1..n); "Salir" is added at the end
     */
    public MainMenuView(InputReader input, List<ConsoleView> modules) {
        this.input = input;
        this.modules = List.copyOf(modules);
    }

    public void show() {
        String[] labels = {"Gestión de Estudiantes", "Gestión Académica", "Gestión de Profesores", "Actividades Extracurriculares",
                "Gestión Financiera", "Gestión de Recursos", "Reportes y Estadísticas"};
        int exitOption = modules.size() + 1;
        while (true) {
            printBanner();
            for (int i = 0; i < modules.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, i < labels.length ? labels[i] : modules.get(i).getTitle());
            }
            System.out.printf("  %d. Salir%n", exitOption);
            int choice;
            try {
                choice = input.readInt("\n  Seleccione una opción: ", 1, exitOption);
            } catch (OperationCancelledException e) {
                continue; // nothing to cancel in the main menu
            }
            if (choice == exitOption) {
                if (input.confirm("  ¿Desea salir del sistema?")) {
                    System.out.println("\n  Gracias por usar el ERP Little Horizons. ¡Hasta pronto!\n");
                    return;
                }
                continue;
            }
            modules.get(choice - 1).show();
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║   ERP Colegio Little Horizons                ║");
        System.out.println("  ║   Sistema de Gestión Educativa               ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println();
    }
}
