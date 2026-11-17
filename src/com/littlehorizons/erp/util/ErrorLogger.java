package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe file logger (Singleton) that appends warnings and errors to {@code error_log.txt}.
 * <p>
 * Each entry contains a timestamp, level, the component that reported it, a message
 * and, for errors, the full stack trace of the exception.
 */
public final class ErrorLogger {

    public enum Level { INFO, WARN, ERROR }

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static volatile ErrorLogger instance;

    private final Path logFile;

    private ErrorLogger(Path logFile) {
        this.logFile = logFile;
    }

    /** Lazy, double-checked locking Singleton. */
    public static ErrorLogger getInstance() {
        if (instance == null) {
            synchronized (ErrorLogger.class) {
                if (instance == null) {
                    instance = new ErrorLogger(AppConfig.getInstance().getLogFile());
                }
            }
        }
        return instance;
    }

    public Path getLogFile() {
        return logFile.toAbsolutePath();
    }

    public void info(String source, String message) {
        write(Level.INFO, source, message, null);
    }

    public void warn(String source, String message) {
        write(Level.WARN, source, message, null);
    }

    public void error(String source, String message, Throwable throwable) {
        write(Level.ERROR, source, message, throwable);
    }

    private synchronized void write(Level level, String source, String message, Throwable throwable) {
        StringBuilder entry = new StringBuilder()
                .append('[').append(LocalDateTime.now().format(TIMESTAMP)).append("] ")
                .append(String.format("%-5s", level)).append(' ')
                .append(source).append(" - ").append(message)
                .append(System.lineSeparator());
        if (throwable != null) {
            StringWriter stack = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stack));
            entry.append(stack).append(System.lineSeparator());
        }
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(entry.toString());
            }
        } catch (IOException e) {
            // Last resort: the log itself failed, report it on stderr without crashing the app.
            System.err.println("No se pudo escribir en el log: " + e.getMessage());
        }
    }
}
