package com.littlehorizons.erp.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Properties;

/**
 * Centralized application configuration.
 * <p>
 * Values are read from {@code config/db.properties} (relative to the working directory).
 * Every key has a sensible default so the application can still start if the file is missing.
 * Implemented as an eager Singleton because configuration is immutable and shared.
 */
public final class AppConfig {

    public static final String CONFIG_FILE = "config/db.properties";

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/erp_little_horizons"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_INVOICE_DIR = "invoices";
    private static final String DEFAULT_LOG_FILE = "error_log.txt";

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties properties = new Properties();

    private AppConfig() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("No se pudo leer " + CONFIG_FILE + "; se usarán valores por defecto.");
            }
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getDatabaseUrl() {
        return properties.getProperty("db.url", DEFAULT_URL).trim();
    }

    public String getDatabaseUser() {
        return properties.getProperty("db.user", DEFAULT_USER).trim();
    }

    public String getDatabasePassword() {
        return properties.getProperty("db.password", DEFAULT_PASSWORD);
    }

    public Path getInvoiceDirectory() {
        return Paths.get(properties.getProperty("app.invoiceDir", DEFAULT_INVOICE_DIR).trim());
    }

    public Path getLogFile() {
        return Paths.get(properties.getProperty("app.logFile", DEFAULT_LOG_FILE).trim());
    }

    /**
     * Academic year used by default in every form. Can be pinned with {@code app.academicYear}
     * (useful to explore the 2026 sample data in a later year).
     */
    public int getCurrentAcademicYear() {
        String value = properties.getProperty("app.academicYear", "").trim();
        if (!value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // fall through to the calendar year
            }
        }
        return LocalDate.now().getYear();
    }
}
