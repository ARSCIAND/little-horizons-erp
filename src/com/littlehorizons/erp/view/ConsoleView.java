package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;

/**
 * Base class of every console screen (Template Method).
 * <p>
 * Subclasses only declare their title and a list of {@link MenuOption}s whose actions are lambdas.
 * This class draws the menu, validates the choice, runs the action and turns any exception
 * into a clear message, so a failure never crashes the application and the user always
 * returns to the same menu.
 */
public abstract class ConsoleView {

    /** A menu entry; an entry with a {@code null} action is rendered as a section title. */
    protected record MenuOption(String label, Runnable action) {
        static MenuOption section(String label) {
            return new MenuOption(label, null);
        }
    }

    protected static final String LINE = "  " + "─".repeat(70);

    protected final InputReader input;

    protected ConsoleView(InputReader input) {
        this.input = input;
    }

    protected abstract String title();

    /** Public accessor used by the main menu. */
    public String getTitle() {
        return title();
    }

    protected abstract List<MenuOption> options();

    /** Shows the menu until the user chooses "0. Volver". */
    public void show() {
        List<MenuOption> options = options();
        List<MenuOption> actions = options.stream().filter(o -> o.action() != null).toList();
        while (true) {
            header(title());
            int number = 1;
            for (MenuOption option : options) {
                if (option.action() == null) {
                    System.out.println("\n  " + option.label().toUpperCase());
                } else {
                    System.out.printf("   %2d. %s%n", number++, option.label());
                }
            }
            System.out.println("\n    0. Volver al menú principal");
            int choice;
            try {
                choice = input.readInt("\n  Seleccione una opción: ", 0, actions.size());
            } catch (OperationCancelledException e) {
                return; // "X" in a menu also goes back
            }
            if (choice == 0) {
                return;
            }
            MenuOption selected = actions.get(choice - 1);
            System.out.println();
            formHeader(selected.label());
            run(selected.action());
        }
    }

    /** Runs an action translating exceptions into user-friendly messages. */
    protected void run(Runnable action) {
        try {
            action.run();
        } catch (OperationCancelledException e) {
            info("Operación cancelada. No se guardaron cambios.");
        } catch (BusinessException e) {
            error(e.getMessage());
        } catch (DataAccessException e) {
            error(e.getMessage());
        } catch (InputClosedException e) {
            throw e;
        } catch (RuntimeException e) {
            ErrorLogger.getInstance().error(getClass().getSimpleName(), "Unexpected error", e);
            error("Ocurrió un error inesperado. El detalle quedó registrado en error_log.txt.");
        }
        input.pause();
    }

    // ------------------------------------------------------------------ output helpers

    protected static void header(String text) {
        System.out.println();
        System.out.println("  ╔" + "═".repeat(68) + "╗");
        System.out.printf("  ║  %-66s║%n", text);
        System.out.println("  ╚" + "═".repeat(68) + "╝");
    }

    protected static void section(String text) {
        System.out.println("  ▸ " + text.toUpperCase());
        System.out.println(LINE);
    }

    /** Header of a form, with the reminder of how to leave it. */
    private static void formHeader(String text) {
        section(text);
        System.out.println("  (Escriba " + InputReader.CANCEL_KEY.toUpperCase() + " y presione Enter en cualquier momento para cancelar y volver)");
        System.out.println();
    }

    protected static void success(String message) {
        System.out.println("\n  ✔ " + message);
    }

    protected static void error(String message) {
        System.out.println("\n  ✖ " + message);
    }

    protected static void info(String message) {
        System.out.println("  ℹ " + message);
    }

    protected static void field(String label, Object value) {
        System.out.printf("    %-24s %s%n", label + ":", value == null || value.toString().isBlank() ? "-" : value);
    }

    /** Asks a value keeping the current one when the user presses Enter (used in update forms). */
    protected String keep(String label, String current, int maxLength) {
        String value = input.readOptionalText("  " + label + " [" + (current == null ? "-" : current) + "]: ", maxLength);
        return value == null ? current : value;
    }

    protected void confirmOrCancel(String question) {
        if (!input.confirm("\n  " + question)) {
            throw new OperationCancelledException();
        }
    }
}
