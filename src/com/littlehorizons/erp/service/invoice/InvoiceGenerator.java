package com.littlehorizons.erp.service.invoice;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for rendering an invoice in a given format. The invoice service iterates over all
 * registered generators, so adding a new format (e.g. XML UBL) only requires a new implementation
 * (Open/Closed Principle).
 */
public interface InvoiceGenerator {

    /** Writes the invoice inside {@code directory} and returns the created file. */
    Path generate(InvoiceData data, Path directory) throws IOException;

    String formatName();
}
