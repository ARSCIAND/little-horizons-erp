package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;

/** Console screens for module 4: inventory and loans. */
public class ResourceView extends ConsoleView {

    private final ResourceController controller;
    private final Lookup lookup;

    public ResourceView(InputReader input, ResourceController controller, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "GESTIÓN DE RECURSOS (BIBLIOTECA Y LABORATORIOS)";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Inventario"),
                new MenuOption("Consultar inventario (por categoría / estado)", this::inventory),
                new MenuOption("Registrar nuevo recurso", this::registerResource),
                new MenuOption("Cambiar estado (mantenimiento, dado de baja, disponible)", this::changeStatus),
                MenuOption.section("Préstamos"),
                new MenuOption("Prestar recurso a un estudiante", () -> lend(BorrowerType.STUDENT)),
                new MenuOption("Prestar recurso a un profesor", () -> lend(BorrowerType.TEACHER)),
                new MenuOption("Registrar devolución", this::registerReturn),
                new MenuOption("Préstamos abiertos (activos y vencidos)", this::openLoans),
                new MenuOption("Reporte de préstamos vencidos", this::overdue),
                new MenuOption("Historial de préstamos de un estudiante", this::studentHistory));
    }

    private void inventory() {
        List<ResourceCategory> categories = controller.listCategories();
        System.out.println("  Categoría: 0. Todas");
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("             %d. %s%n", i + 1, categories.get(i).getName());
        }
        int c = input.readInt("  Seleccione: ", 0, categories.size());
        System.out.println("  Estado: 0. Todos  1. Disponible  2. Prestado  3. En Mantenimiento  4. Dado de Baja");
        int s = input.readInt("  Seleccione: ", 0, 4);
        printResources(controller.listResources(c == 0 ? null : categories.get(c - 1).getId(), s == 0 ? null : ResourceStatus.values()[s - 1]));
    }

    private void printResources(List<Resource> resources) {
        TablePrinter.of(resources)
                .column("ID", Resource::getId)
                .column("Código", Resource::getCode)
                .column("Recurso", Resource::getName)
                .column("Categoría", Resource::getCategoryName)
                .column("Ubicación", Resource::getLocation)
                .column("Estado", Resource::getStatus)
                .print();
    }

    private void registerResource() {
        List<ResourceCategory> categories = controller.listCategories();
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, categories.get(i).getName());
        }
        ResourceCategory category = categories.get(input.readInt("  Categoría: ", 1, categories.size()) - 1);
        Resource r = new Resource();
        r.setName(input.readText("  Nombre del recurso *: ", 3, 150));
        r.setDescription(input.readOptionalText("  Descripción: ", 500));
        r.setLocation(input.readText("  Ubicación física * (ej: Biblioteca, Laboratorio 1): ", 3, 100));
        r.setAcquisitionDate(input.readDateOrDefault("  Fecha de adquisición", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura."));
        Resource saved = controller.registerResource(r, category);
        success("Recurso registrado con código único " + saved.getCode() + " (ID " + saved.getId() + ").");
    }

    private Resource pickResource(ResourceStatus filter) {
        String term = input.readLine("  Código del recurso (ej: LIB-001) o Enter para ver la lista: ");
        if (!term.isBlank()) {
            return controller.findByCode(term);
        }
        printResources(controller.listResources(null, filter));
        return controller.getResource(input.readInt("  ID del recurso: ", 1, Integer.MAX_VALUE));
    }

    private void changeStatus() {
        Resource r = pickResource(null);
        info(r.getCode() + " - " + r.getName() + " | estado actual: " + r.getStatus());
        ResourceStatus status = input.readEnum("Nuevo estado", new ResourceStatus[]{ResourceStatus.AVAILABLE, ResourceStatus.MAINTENANCE, ResourceStatus.DECOMMISSIONED});
        confirmOrCancel("¿Cambiar el estado a " + status + "?");
        controller.changeStatus(r.getId(), status);
        success("Estado actualizado.");
    }

    private void lend(BorrowerType type) {
        int borrowerId = type == BorrowerType.STUDENT ? lookup.student().getId() : lookup.teacher().getId();
        controller.checkCanBorrow(type, borrowerId);
        Resource r = controller.requireAvailable(pickResource(ResourceStatus.AVAILABLE).getId());
        LocalDate due = input.readDateOrDefault("  Fecha de devolución programada", LocalDate.now().plusDays(14),
                d -> d.isAfter(LocalDate.now()), "Debe ser una fecha futura.");
        String notes = input.readOptionalText("  Observaciones: ", 255);
        int loanId = controller.lend(type, r.getId(), borrowerId, due, notes);
        success("Préstamo N° " + loanId + " registrado: " + r.getCode() + " - " + r.getName() + ". Devolver antes del " + due + ".");
    }

    private void registerReturn() {
        List<Loan> open = controller.openLoans();
        printLoans(open);
        if (open.isEmpty()) {
            return;
        }
        String code = input.readLine("  Código del recurso devuelto: ").toUpperCase();
        Loan loan = open.stream().filter(l -> l.getResourceCode().equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new BusinessException("No hay un préstamo abierto para el recurso " + code + "."));
        LocalDate date = input.readDateOrDefault("  Fecha real de devolución", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura.");
        String notes = input.readOptionalText("  Estado en que se recibe / observaciones: ", 255);
        controller.registerReturn(loan.getBorrowerType(), loan.getId(), date, notes);
        long late = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), date));
        success("Devolución registrada. " + loan.getResourceCode() + " vuelve a estar Disponible."
                + (late > 0 ? " Se devolvió con " + late + " día(s) de retraso." : ""));
    }

    private void openLoans() {
        printLoans(controller.openLoans());
    }

    private void overdue() {
        LocalDate today = LocalDate.now();
        List<Loan> loans = controller.overdueLoans(today);
        TablePrinter.of(loans)
                .column("Tipo", l -> l.getBorrowerType().getLabel())
                .column("Prestatario", Loan::getBorrowerName)
                .column("Código", Loan::getResourceCode)
                .column("Recurso", Loan::getResourceName)
                .column("Prestado", l -> Formats.date(l.getLoanDate()))
                .column("Debía volver", l -> Formats.date(l.getDueDate()))
                .column("Días de retraso", l -> l.getDaysOverdue(today))
                .print();
    }

    private void studentHistory() {
        int id = lookup.student().getId();
        printLoans(controller.loanHistory(BorrowerType.STUDENT, id));
    }

    private void printLoans(List<Loan> loans) {
        TablePrinter.of(loans)
                .column("N°", Loan::getId)
                .column("Tipo", l -> l.getBorrowerType().getLabel())
                .column("Prestatario", Loan::getBorrowerName)
                .column("Código", Loan::getResourceCode)
                .column("Recurso", Loan::getResourceName)
                .column("Préstamo", l -> Formats.date(l.getLoanDate()))
                .column("Devolución prog.", l -> Formats.date(l.getDueDate()))
                .column("Devolución real", l -> Formats.date(l.getReturnDate()))
                .column("Estado", Loan::getStatus)
                .print();
    }
}
