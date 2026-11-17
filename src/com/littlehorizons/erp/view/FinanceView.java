package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.FinanceController.PaymentResult;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.CustomPrice;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.report.AmountSummary;
import com.littlehorizons.erp.model.report.DelinquentAccount;
import com.littlehorizons.erp.service.invoice.InvoiceService.IssuedInvoice;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Console screens for module 3: concepts, payments, invoices and delinquency. */
public class FinanceView extends ConsoleView {

    private final FinanceController controller;
    private final StudentController students;
    private final Lookup lookup;

    public FinanceView(InputReader input, FinanceController controller, StudentController students, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.students = students;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "GESTIÓN FINANCIERA";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Conceptos de pago"),
                new MenuOption("Catálogo de conceptos", this::listConcepts),
                new MenuOption("Crear concepto de pago", this::createConcept),
                new MenuOption("Actualizar valor base / activar-desactivar concepto", this::updateConcept),
                new MenuOption("Asignar valor personalizado a un estudiante (beca, descuento)", this::setCustomPrice),
                new MenuOption("Listar / eliminar valores personalizados", this::customPrices),
                MenuOption.section("Pagos y facturación electrónica"),
                new MenuOption("Registrar pago y generar factura electrónica", this::registerPayment),
                new MenuOption("Historial de pagos de un estudiante", this::historyByStudent),
                new MenuOption("Historial de pagos de un acudiente (todos sus hijos)", this::historyByGuardian),
                new MenuOption("Generar factura de un pago existente", this::invoiceExisting),
                new MenuOption("Facturas emitidas", this::listInvoices),
                MenuOption.section("Cartera"),
                new MenuOption("Reporte de morosos (pensiones pendientes > 1 mes)", this::delinquents),
                new MenuOption("Recaudo del año por concepto y medio de pago", this::income));
    }

    // ------------------------------------------------------------------ concepts

    private void listConcepts() {
        printConcepts(controller.listConcepts(false));
    }

    private void printConcepts(List<PaymentConcept> concepts) {
        TablePrinter.of(concepts)
                .column("ID", PaymentConcept::getId)
                .column("Código", PaymentConcept::getCode)
                .column("Concepto", PaymentConcept::getName)
                .column("Valor base", c -> Formats.money(c.getBaseAmount()))
                .column("Recurrente", c -> c.isRecurring() ? "Mensual" : "Único")
                .column("Activo", c -> c.isActive() ? "Sí" : "No")
                .print();
    }

    private PaymentConcept pickConcept(boolean onlyActive) {
        List<PaymentConcept> concepts = controller.listConcepts(onlyActive);
        printConcepts(concepts);
        int id = input.readInt("  ID del concepto: ", 1, Integer.MAX_VALUE);
        return concepts.stream().filter(c -> c.getId() == id).findFirst()
                .orElseThrow(() -> new BusinessException("El concepto no está en la lista."));
    }

    private void createConcept() {
        PaymentConcept c = new PaymentConcept();
        c.setCode(input.readMatching("  Código * (MAYÚSCULAS, ej: ROBOTICA): ", java.util.regex.Pattern.compile("^[A-Za-z_]{3,20}$"),
                "Use de 3 a 20 letras sin espacios.", false));
        c.setName(input.readText("  Nombre *: ", 3, 100));
        c.setDescription(input.readOptionalText("  Descripción: ", 255));
        c.setBaseAmount(input.readMoney("  Valor base en pesos *: ", BigDecimal.ZERO));
        c.setRecurring(input.confirm("  ¿Es un cobro mensual recurrente?"));
        PaymentConcept saved = controller.createConcept(c);
        success("Concepto " + saved.getCode() + " creado con ID " + saved.getId() + ".");
    }

    private void updateConcept() {
        PaymentConcept c = pickConcept(false);
        c.setBaseAmount(input.readOptionalInt("  Nuevo valor base [" + Formats.money(c.getBaseAmount()) + "] (Enter = conservar): ", 0, 99_999_999)
                .map(BigDecimal::valueOf).orElse(c.getBaseAmount()));
        c.setActive(input.confirm("  ¿El concepto queda activo?"));
        controller.updateConcept(c);
        success("Concepto actualizado.");
    }

    private void setCustomPrice() {
        controller.requireCustomPrices();
        Student s = lookup.student();
        PaymentConcept c = pickConcept(true);
        int year = lookup.year();
        info("Valor base: " + Formats.money(c.getBaseAmount()) + " | valor actual para el estudiante: "
                + Formats.money(controller.resolvePrice(s.getId(), c.getId(), year)));
        BigDecimal amount = input.readMoney("  Valor personalizado: ", BigDecimal.ZERO);
        String reason = input.readText("  Motivo (ej: Descuento 10% por hermanos): ", 3, 150);
        controller.setCustomPrice(s.getId(), c.getId(), year, amount, reason);
        success("Valor personalizado guardado: " + c.getName() + " = " + Formats.money(amount) + " para " + s.getShortName() + ".");
    }

    private void customPrices() {
        controller.requireCustomPrices();
        int year = lookup.year();
        List<CustomPrice> prices = controller.listCustomPrices(year);
        TablePrinter.of(prices)
                .column("ID", CustomPrice::getId)
                .column("Estudiante", CustomPrice::getStudentName)
                .column("Concepto", CustomPrice::getConceptName)
                .column("Valor", p -> Formats.money(p.getAmount()))
                .column("Motivo", CustomPrice::getReason)
                .print();
        if (!prices.isEmpty() && input.confirm("  ¿Desea eliminar alguno?")) {
            int id = input.readInt("  ID del valor personalizado: ", 1, Integer.MAX_VALUE);
            controller.removeCustomPrice(id);
            success("Valor personalizado eliminado; se cobrará el valor base.");
        }
    }

    // ------------------------------------------------------------------ payments

    private void registerPayment() {
        Student s = lookup.student();
        PaymentConcept concept = pickConcept(true);
        int year = controller.currentYear();
        BigDecimal suggested = controller.resolvePrice(s.getId(), concept.getId(), year);
        info("Valor a cobrar para este estudiante: " + Formats.money(suggested));
        BigDecimal amount = input.confirm("  ¿Usar ese valor?") ? suggested : input.readMoney("  Monto pagado: ", BigDecimal.ONE);

        List<Guardian> guardians = controller.getGuardians(s.getId());
        Guardian payer = controller.getPayer(s.getId());
        if (guardians.size() > 1 && !input.confirm("  Factura a nombre de " + payer.getFullName() + ". ¿Correcto?")) {
            TablePrinter.of(guardians).column("ID", Guardian::getId).column("Acudiente", Guardian::getFullName)
                    .column("Parentesco", Guardian::getRelationship).print();
            int id = input.readInt("  ID del acudiente que paga: ", 1, Integer.MAX_VALUE);
            payer = guardians.stream().filter(g -> g.getId() == id).findFirst()
                    .orElseThrow(() -> new BusinessException("El acudiente no está vinculado al estudiante."));
        }
        LocalDate date = input.readDateOrDefault("  Fecha del pago", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura.");
        PaymentMethod method = input.readEnum("Método de pago", PaymentMethod.values());
        PaymentPeriod period = input.readEnum("Período al que aplica", PaymentPeriod.values());
        String notes = input.readOptionalText("  Detalle (ej: Pensión noviembre 2026): ", 255);

        System.out.println();
        section("Confirmación");
        field("Estudiante", s.getFullName());
        field("Concepto", concept.getName());
        field("Monto", Formats.money(amount));
        field("Pagado por", payer.getFullName());
        field("Fecha / método", date + " / " + method);
        confirmOrCancel("¿Confirmar el pago?");

        PaymentResult result = controller.registerPayment(s.getId(), concept.getId(), payer.getId(), amount, date, method, period, notes);
        success("Pago registrado. Recibo " + result.payment().getReceiptNumber() + ".");
        if (result.invoice() != null) {
            printIssued(result.invoice());
        } else {
            error("El pago quedó guardado pero la factura no se generó: " + result.invoiceError()
                    + " Use 'Generar factura de un pago existente'.");
        }
    }

    private void printIssued(IssuedInvoice issued) {
        success("Factura electrónica " + issued.invoice().getInvoiceNumber() + " generada.");
        field("CUFE", issued.invoice().getCufe().substring(0, 32) + "...");
        for (Path file : issued.files()) {
            field("Archivo", file);
        }
    }

    private void historyByStudent() {
        Student s = lookup.student();
        List<Payment> payments = controller.historyByStudent(s.getId());
        printPayments(payments);
        info("Total pagado: " + Formats.money(controller.total(payments)));
    }

    private void historyByGuardian() {
        String document = input.readDocument("  Documento del acudiente: ");
        Guardian guardian = students.findGuardianByDocument(document)
                .orElseThrow(() -> new BusinessException("No existe un acudiente con ese documento."));
        section("Pagos asociados a " + guardian.getFullName());
        List<Payment> payments = controller.historyByGuardian(guardian.getId());
        printPayments(payments);
        info("Total pagado por la familia: " + Formats.money(controller.total(payments)));
    }

    private void printPayments(List<Payment> payments) {
        TablePrinter.of(payments)
                .column("ID", Payment::getId)
                .column("Fecha", p -> Formats.date(p.getPaymentDate()))
                .column("Recibo", Payment::getReceiptNumber)
                .column("Estudiante", Payment::getStudentName)
                .column("Concepto", Payment::getConceptName)
                .column("Monto", p -> Formats.money(p.getAmount()))
                .column("Método", Payment::getMethod)
                .column("Factura", Payment::getInvoiceNumber)
                .print();
    }

    private void invoiceExisting() {
        info("Últimos pagos sin factura:");
        printPayments(controller.paymentsWithoutInvoice(15));
        int id = input.readInt("  ID del pago a facturar (0 = cancelar): ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            throw new com.littlehorizons.erp.exception.OperationCancelledException();
        }
        printIssued(controller.issueInvoice(id));
    }

    private void listInvoices() {
        TablePrinter.of(controller.listInvoices())
                .column("Número", Invoice::getInvoiceNumber)
                .column("Emisión", i -> Formats.date(i.getIssueDate()))
                .column("Estudiante", Invoice::getStudentName)
                .column("Valor", i -> Formats.money(i.getAmount()))
                .column("Archivo", Invoice::getFileName)
                .column("CUFE", i -> i.getCufe().substring(0, 16) + "…")
                .print();
    }

    // ------------------------------------------------------------------ portfolio

    private void delinquents() {
        int year = lookup.year();
        LocalDate today = LocalDate.now();
        List<DelinquentAccount> accounts = controller.delinquentAccounts(year, today);
        info("Pensiones exigibles con más de un mes de vencidas al " + today + ": "
                + FinanceController.monthsOverdueThreshold(year, today) + " (vencen el día 5 de cada mes).");
        TablePrinter.of(accounts)
                .column("ID", DelinquentAccount::studentId)
                .column("Estudiante", DelinquentAccount::studentName)
                .column("Grado", DelinquentAccount::gradeName)
                .column("Pagadas", DelinquentAccount::monthsPaid)
                .column("Pendientes", DelinquentAccount::monthsPending)
                .column("Cuota", a -> Formats.money(a.monthlyFee()))
                .column("Deuda", a -> Formats.money(a.amountPending()))
                .column("Último pago", a -> Formats.date(a.lastPaymentDate()))
                .column("Acudiente", DelinquentAccount::guardianName)
                .column("Teléfono", DelinquentAccount::guardianPhone)
                .print();
        BigDecimal total = accounts.stream().map(DelinquentAccount::amountPending).reduce(BigDecimal.ZERO, BigDecimal::add);
        info("Cartera vencida total: " + Formats.money(total));
    }

    private void income() {
        int year = lookup.year();
        section("Recaudo por concepto " + year);
        printSummary(controller.incomeByConcept(year));
        System.out.println();
        section("Recaudo por medio de pago " + year);
        printSummary(controller.incomeByMethod(year));
    }

    private void printSummary(List<AmountSummary> rows) {
        TablePrinter.of(rows)
                .column("Concepto / medio", AmountSummary::label)
                .column("N° pagos", AmountSummary::count)
                .column("Total", r -> Formats.money(r.total()))
                .print();
        info("Gran total: " + Formats.money(rows.stream().map(AmountSummary::total).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }
}
