package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that centralizes how JDBC connections are created.
 * <p>
 * It does not keep a single shared {@link Connection}; instead it hands out a fresh connection
 * per operation so every DAO can close it with try-with-resources. This keeps the code safe if
 * the server drops idle connections and makes transactions independent from each other.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection(AppConfig config) {
        this.url = config.getDatabaseUrl();
        this.user = config.getDatabaseUser();
        this.password = config.getDatabasePassword();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection(AppConfig.getInstance());
                }
            }
        }
        return instance;
    }

    /** Opens a new connection. The caller is responsible for closing it (try-with-resources). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Verifies connectivity at startup.
     *
     * @throws DataAccessException with a user friendly explanation when the database is unreachable
     */
    public void testConnection() {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(5)) {
                throw new DataAccessException("La conexión con la base de datos no es válida.");
            }
        } catch (SQLException e) {
            ErrorLogger.getInstance().error("DatabaseConnection", "Connection test failed for " + url, e);
            throw new DataAccessException(buildHint(e), e);
        }
    }

    public String getUrl() {
        return url;
    }

    private String buildHint(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("No suitable driver")) {
            return "No se encontró el driver JDBC de MySQL. Agregue mysql-connector-j a la carpeta lib/ (ver README, sección 6.3).";
        }
        if (message.contains("Access denied")) {
            return "Usuario o contraseña de MySQL incorrectos. Revise " + AppConfig.CONFIG_FILE + ".";
        }
        if (message.contains("Unknown database")) {
            return "La base de datos erp_little_horizons no existe. Ejecute docs/database.sql.";
        }
        return "No fue posible conectarse a MySQL (" + message + "). Verifique que el servidor esté encendido.";
    }
}
