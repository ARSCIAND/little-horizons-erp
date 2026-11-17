package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.config.SchoolInfo;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.NumberToWords;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the electronic invoice flow (requirement 3.3):
 * collect data from the database → compute VAT and CUFE → render every format → persist the invoice row.
 */
public class InvoiceService {

    /** Result returned to the caller: the stored invoice and the files that were written. */
    public record IssuedInvoice(Invoice invoice, List<Path> files) {
    }

    private final PaymentDao paymentDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final InvoiceDao invoiceDao;
    private final List<InvoiceGenerator> generators;
    private final Path outputDirectory;

    public InvoiceService(PaymentDao paymentDao, StudentDao studentDao, GuardianDao guardianDao, InvoiceDao invoiceDao,
                          List<InvoiceGenerator> generators) {
        this.paymentDao = paymentDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.invoiceDao = invoiceDao;
        this.generators = List.copyOf(generators);
        this.outputDirectory = AppConfig.getInstance().getInvoiceDirectory();
    }

    public IssuedInvoice issueFor(int paymentId) {
        invoiceDao.findByPayment(paymentId).ifPresent(existing -> {
            throw new BusinessException("El pago ya tiene la factura " + existing.getInvoiceNumber() + " (" + existing.getFilePath() + ").");
        });
        Payment payment = paymentDao.findById(paymentId)
                .orElseThrow(() -> new BusinessException("No existe el pago con ID " + paymentId + "."));
        Student student = studentDao.findById(payment.getStudentId())
                .orElseThrow(() -> new BusinessException("El estudiante del pago no existe."));
        Guardian payer = (payment.getGuardianId() != null ? guardianDao.findById(payment.getGuardianId()) : guardianDao.findPayer(student.getId()))
                .orElseThrow(() -> new BusinessException("El estudiante no tiene un acudiente responsable para facturar."));

        InvoiceData data = buildData(payment, student, payer);
        List<Path> files = new ArrayList<>();
        try {
            for (InvoiceGenerator generator : generators) {
                files.add(generator.generate(data, outputDirectory).toAbsolutePath());
            }
        } catch (IOException e) {
            ErrorLogger.getInstance().error("InvoiceService", "Could not write invoice files for payment " + paymentId, e);
            throw new BusinessException("No se pudo escribir el archivo de la factura en " + outputDirectory.toAbsolutePath() + ".");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(data.invoiceNumber());
        invoice.setPaymentId(paymentId);
        invoice.setIssueDate(data.issuedAt().toLocalDate());
        invoice.setCufe(data.cufe());
        invoice.setFileName(data.baseFileName() + ".txt");
        invoice.setFilePath(files.isEmpty() ? null : files.get(0).toString());
        invoice.setId(invoiceDao.save(invoice));
        invoice.setStudentName(student.getFullName());
        invoice.setAmount(payment.getAmount());
        ErrorLogger.getInstance().info("InvoiceService", "Invoice " + invoice.getInvoiceNumber() + " issued for payment " + paymentId);
        return new IssuedInvoice(invoice, files);
    }

    private InvoiceData buildData(Payment payment, Student student, Guardian payer) {
        long sequence = invoiceDao.nextSequence();
        String number = String.format("%s-%06d", SchoolInfo.INVOICE_PREFIX, sequence);
        LocalDateTime issuedAt = LocalDateTime.now().withNano(0);

        boolean taxable = SchoolInfo.TAXABLE_CONCEPT_CODES.contains(payment.getConceptCode());
        BigDecimal total = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        // Amounts are charged VAT-included; the base is extracted for taxable concepts.
        BigDecimal subtotal = taxable
                ? total.divide(BigDecimal.ONE.add(SchoolInfo.VAT_RATE), 2, RoundingMode.HALF_UP)
                : total;
        BigDecimal vat = total.subtract(subtotal);
        BigDecimal ratePercent = taxable ? SchoolInfo.VAT_RATE.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        String description = payment.getConceptName() + (payment.getNotes() != null ? " - " + payment.getNotes() : "");
        InvoiceData.Line line = new InvoiceData.Line(payment.getConceptCode(), description, 1, subtotal, subtotal);
        String cufe = CufeGenerator.generate(number, issuedAt, subtotal, vat, total, SchoolInfo.NIT_DIGITS,
                payer.getDocumentNumber(), SchoolInfo.DIAN_TECHNICAL_KEY);
        String note = taxable ? "Valor incluye IVA del 19%." : "Servicio educativo excluido de IVA (Art. 476 num. 6 del Estatuto Tributario).";

        InvoiceData.Customer customer = new InvoiceData.Customer(payer.getFullName(), payer.getDocumentType().getDbValue(),
                payer.getDocumentNumber(), payer.getAddress(), payer.getPhone(), payer.getEmail());
        return new InvoiceData(number, sequence, issuedAt, customer, student.getFullName(), student.getCurrentGrade(),
                payment.getReceiptNumber(), payment.getMethod().getDbValue(), List.of(line), subtotal, vat, ratePercent,
                total, NumberToWords.pesos(total), cufe, SchoolInfo.DIAN_QR_BASE_URL + cufe, note);
    }
}
