package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.config.SchoolInfo;
import com.littlehorizons.erp.util.Formats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the graphic representation of the electronic invoice as a UTF-8 text file
 * (factura_000123.txt) that can be opened with any editor and printed on POS printers.
 * The simulated QR code is drawn with block characters.
 */
public class TextInvoiceGenerator implements InvoiceGenerator {

    private static final int WIDTH = 78;

    @Override
    public Path generate(InvoiceData d, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(d.baseFileName() + ".txt");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            String rule = "=".repeat(WIDTH);
            String thin = "-".repeat(WIDTH);
            out.write(rule + "\n");
            out.write(center(SchoolInfo.LEGAL_NAME.toUpperCase()) + "\n");
            out.write(center("NIT " + SchoolInfo.NIT + " - " + SchoolInfo.TAX_REGIME) + "\n");
            out.write(center(SchoolInfo.ADDRESS) + "\n");
            out.write(center("Tel: " + SchoolInfo.PHONE + "  |  " + SchoolInfo.EMAIL) + "\n");
            out.write(rule + "\n");
            out.write(center("FACTURA ELECTRÓNICA DE VENTA N° " + d.invoiceNumber()) + "\n");
            out.write(center("Resolución DIAN " + SchoolInfo.DIAN_RESOLUTION_NUMBER + " del " + SchoolInfo.DIAN_RESOLUTION_DATE
                    + " - Rango " + SchoolInfo.DIAN_RANGE) + "\n");
            out.write(center("Vigencia: " + SchoolInfo.DIAN_RESOLUTION_VALIDITY + " (simulada)") + "\n");
            out.write(thin + "\n");
            out.write(String.format("Fecha de emisión : %s%n", d.issuedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            out.write(String.format("Forma de pago    : Contado        Medio de pago: %s%n", d.paymentMethod()));
            out.write(String.format("Recibo de caja   : %s%n", d.receiptNumber()));
            out.write(thin + "\n");
            out.write("ADQUIRIENTE\n");
            out.write(String.format("  Nombre    : %s%n", d.customer().fullName()));
            out.write(String.format("  Documento : %s %s%n", d.customer().documentType(), d.customer().documentNumber()));
            out.write(String.format("  Dirección : %s%n", Formats.nullSafe(d.customer().address())));
            out.write(String.format("  Teléfono  : %s   Correo: %s%n", Formats.nullSafe(d.customer().phone()), Formats.nullSafe(d.customer().email())));
            out.write(String.format("  Estudiante: %s (%s)%n", d.studentName(), Formats.nullSafe(d.studentGrade())));
            out.write(thin + "\n");
            out.write(String.format("%-3s %-42s %4s %13s %13s%n", "N°", "Descripción", "Cant", "Vr. unitario", "Subtotal"));
            out.write(thin + "\n");
            int index = 1;
            for (InvoiceData.Line line : d.lines()) {
                out.write(String.format("%-3d %-42s %4d %13s %13s%n", index++, clip(line.description(), 42), line.quantity(),
                        Formats.money(line.unitPrice()), Formats.money(line.subtotal())));
            }
            out.write(thin + "\n");
            out.write(String.format("%62s %15s%n", "Subtotal:", Formats.money(d.subtotal())));
            out.write(String.format("%62s %15s%n", "IVA (" + d.vatRatePercent().stripTrailingZeros().toPlainString() + "%):", Formats.money(d.vat())));
            out.write(String.format("%62s %15s%n", "TOTAL A PAGAR:", Formats.money(d.total())));
            out.write("Son: " + d.totalInWords() + "\n");
            out.write(thin + "\n");
            out.write("CUFE: " + d.cufe() + "\n");
            out.write("Consulta (QR): " + d.qrContent() + "\n\n");
            out.write(center("Código QR (simulado)") + "\n");
            for (String row : qrRows(d.qrContent())) {
                out.write(center(row) + "\n");
            }
            out.write("\n");
            if (d.notes() != null) {
                out.write("Nota: " + d.notes() + "\n");
            }
            out.write(rule + "\n");
            out.write(center("Representación gráfica de factura electrónica - documento simulado con fines académicos") + "\n");
            out.write(rule + "\n");
        }
        return file;
    }

    @Override
    public String formatName() {
        return "TXT";
    }

    /** Draws the simulated QR matrix with a quiet zone, two characters per module so it looks square. */
    private static List<String> qrRows(String content) {
        boolean[][] modules = SimulatedQrGenerator.generate(content);
        List<String> rows = new ArrayList<>();
        String quiet = "  ".repeat(modules.length + 2);
        rows.add(quiet);
        for (boolean[] row : modules) {
            StringBuilder line = new StringBuilder("  ");
            for (boolean dark : row) {
                line.append(dark ? "██" : "  ");
            }
            rows.add(line.append("  ").toString());
        }
        rows.add(quiet);
        return rows;
    }

    private static String center(String text) {
        int padding = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
