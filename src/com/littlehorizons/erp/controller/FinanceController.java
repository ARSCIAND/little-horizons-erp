package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.CustomPrice;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.AmountSummary;
import com.littlehorizons.erp.model.report.DelinquentAccount;
import com.littlehorizons.erp.service.invoice.InvoiceService;
import com.littlehorizons.erp.service.invoice.InvoiceService.IssuedInvoice;
import com.littlehorizons.erp.util.ErrorLogger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Business rules for payment concepts, payments, invoices and delinquency (module 3). */
public class FinanceController {

    /** Monthly fees are billed from February to November (Colombian calendar A) and fall due on day 5. */
    private static final int FIRST_FEE_MONTH = Month.FEBRUARY.getValue();
    private static final int LAST_FEE_MONTH = Month.NOVEMBER.getValue();
    private static final int FEE_DUE_DAY = 5;

    /** Outcome of registering a payment: the payment plus the invoice (or the reason it could not be issued). */
    public record PaymentResult(Payment payment, IssuedInvoice invoice, String invoiceError) {
    }

    private final PaymentConceptDao conceptDao;
    private final PaymentDao paymentDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final InvoiceDao invoiceDao;
    private final InvoiceService invoiceService;

    public FinanceController(PaymentConceptDao conceptDao, PaymentDao paymentDao, StudentDao studentDao, GuardianDao guardianDao,
                             InvoiceDao invoiceDao, InvoiceService invoiceService) {
        this.conceptDao = conceptDao;
        this.paymentDao = paymentDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.invoiceDao = invoiceDao;
        this.invoiceService = invoiceService;
    }

    public int currentYear() {
        return AppConfig.getInstance().getCurrentAcademicYear();
    }

    // ------------------------------------------------------------------ concepts

    public List<PaymentConcept> listConcepts(boolean onlyActive) {
        return onlyActive ? conceptDao.findActive() : conceptDao.findAll();
    }

    public PaymentConcept getConcept(int id) {
        return conceptDao.findById(id).orElseThrow(() -> new BusinessException("No existe el concepto con ID " + id + "."));
    }

    public PaymentConcept createConcept(PaymentConcept concept) {
        concept.setCode(concept.getCode().toUpperCase());
        if (conceptDao.findByCode(concept.getCode()).isPresent()) {
            throw new BusinessException("Ya existe un concepto con el código " + concept.getCode() + ".");
        }
        concept.setActive(true);
        concept.setId(conceptDao.save(concept));
        return concept;
    }

    public void updateConcept(PaymentConcept concept) {
        conceptDao.update(concept);
    }

    // ------------------------------------------------------------------ custom prices

    public void setCustomPrice(int studentId, int conceptId, int year, BigDecimal amount, String reason) {
        requireCustomPrices();
        Student student = getStudent(studentId);
        PaymentConcept concept = getConcept(conceptId);
        if (amount.compareTo(concept.getBaseAmount()) > 0) {
            throw new BusinessException("El valor personalizado no puede superar el valor base (" + concept.getBaseAmount() + ").");
        }
        CustomPrice price = new CustomPrice();
        price.setStudentId(student.getId());
        price.setConceptId(concept.getId());
        price.setAcademicYear(year);
        price.setAmount(amount);
        price.setReason(reason);
        conceptDao.saveCustomPrice(price);
    }

    public List<CustomPrice> listCustomPrices(int year) {
        return conceptDao.findCustomPrices(year);
    }

    public void removeCustomPrice(int customPriceId) {
        requireCustomPrices();
        if (!conceptDao.deleteCustomPrice(customPriceId)) {
            throw new BusinessException("No existe el valor personalizado " + customPriceId + ".");
        }
    }

    /** Price to charge a student: the custom value for the year if any, otherwise the base value. */
    public BigDecimal resolvePrice(int studentId, int conceptId, int year) {
        return conceptDao.findCustomPrice(studentId, conceptId, year)
                .map(CustomPrice::getAmount)
                .orElseGet(() -> getConcept(conceptId).getBaseAmount());
    }

    // ------------------------------------------------------------------ payments

    public Guardian getPayer(int studentId) {
        return guardianDao.findPayer(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene acudientes registrados."));
    }

    public List<Guardian> getGuardians(int studentId) {
        return guardianDao.findByStudent(studentId);
    }

    /**
     * Registers a payment and immediately issues its electronic invoice as a .txt file.
     * If the invoice cannot be generated the payment is kept and the error is reported,
     * so the invoice can be generated later from the menu.
     */
    public PaymentResult registerPayment(int studentId, int conceptId, Integer guardianId, BigDecimal amount, LocalDate date,
                                         PaymentMethod method, PaymentPeriod period, String notes) {
        Student student = getStudent(studentId);
        if (student.getStatus() == StudentStatus.WITHDRAWN) {
            throw new BusinessException("El estudiante está retirado; no se pueden registrar nuevos cobros.");
        }
        PaymentConcept concept = getConcept(conceptId);
        if (!concept.isActive()) {
            throw new BusinessException("El concepto " + concept.getName() + " está inactivo.");
        }
        if (amount.signum() <= 0) {
            throw new BusinessException("El monto debe ser mayor que cero.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de pago no puede ser futura.");
        }
        Guardian payer = guardianId == null ? getPayer(studentId)
                : guardianDao.findByStudent(studentId).stream().filter(g -> g.getId() == guardianId.intValue()).findFirst()
                .orElseThrow(() -> new BusinessException("El acudiente seleccionado no está vinculado al estudiante."));

        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setGuardianId(payer.getId());
        payment.setConceptId(conceptId);
        payment.setAmount(amount);
        payment.setPaymentDate(date);
        payment.setMethod(method);
        payment.setAcademicYear(date.getYear());
        payment.setPeriod(period);
        payment.setNotes(notes);
        payment.setReceiptNumber(paymentDao.nextReceiptNumber(date.getYear()));
        int paymentId = paymentDao.save(payment);
        Payment saved = paymentDao.findById(paymentId).orElseThrow();

        try {
            return new PaymentResult(saved, invoiceService.issueFor(paymentId), null);
        } catch (BusinessException e) {
            ErrorLogger.getInstance().warn("FinanceController", "Payment " + paymentId + " saved without invoice: " + e.getMessage());
            return new PaymentResult(saved, null, e.getMessage());
        }
    }

    public IssuedInvoice issueInvoice(int paymentId) {
        return invoiceService.issueFor(paymentId);
    }

    public List<Payment> paymentsWithoutInvoice(int limit) {
        return paymentDao.findWithoutInvoice(limit);
    }

    public List<Invoice> listInvoices() {
        return invoiceDao.findAll();
    }

    public List<Payment> historyByStudent(int studentId) {
        getStudent(studentId);
        return paymentDao.findByStudent(studentId);
    }

    public List<Payment> historyByGuardian(int guardianId) {
        guardianDao.findById(guardianId).orElseThrow(() -> new BusinessException("El acudiente no existe."));
        return paymentDao.findByGuardian(guardianId);
    }

    public BigDecimal total(List<Payment> payments) {
        return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ delinquency

    /** Number of monthly fees of {@code year} whose due date (day 5) is more than one month in the past at {@code today}. */
    public static int monthsOverdueThreshold(int year, LocalDate today) {
        return (int) IntStream.rangeClosed(FIRST_FEE_MONTH, LAST_FEE_MONTH)
                .mapToObj(month -> LocalDate.of(year, month, FEE_DUE_DAY))
                .filter(dueDate -> dueDate.plusMonths(1).isBefore(today))
                .count();
    }

    /** Students with monthly fees pending for more than one month (requirement 3.2), most indebted first. */
    public List<DelinquentAccount> delinquentAccounts(int year, LocalDate today) {
        int monthsDue = monthsOverdueThreshold(year, today);
        return paymentDao.findPensionStatus(year, monthsDue).stream()
                .filter(account -> account.monthsPending() > 0)
                .sorted(Comparator.comparing(DelinquentAccount::amountPending).reversed()
                        .thenComparing(DelinquentAccount::studentName))
                .toList();
    }

    public List<AmountSummary> incomeByConcept(int year) {
        return paymentDao.sumByConcept(year);
    }

    public List<AmountSummary> incomeByMethod(int year) {
        return paymentDao.sumByMethod(year);
    }

    /** Fails fast when the optional table valores_personalizados is not installed. */
    public void requireCustomPrices() {
        if (!conceptDao.customPricesAvailable()) {
            throw new BusinessException("Los valores personalizados requieren la tabla valores_personalizados "
                    + "(extensión 1B de docs/database.sql). Ejecute docs/database.sql completo para habilitarlos.");
        }
    }

    private Student getStudent(int studentId) {
        return studentDao.findById(studentId).orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
    }
}
