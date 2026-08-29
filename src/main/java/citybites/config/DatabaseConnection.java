package citybites.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a single JDBC connection for the application lifetime.
 * Call DatabaseConnection.get() to obtain the connection.
 * The connection is opened lazily on first use and reused thereafter.
 */
public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static Connection connection;

    private DatabaseConnection() {}

    /**
     * Returns the shared connection, opening it if not yet open.
     */
    public static Connection get() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = openConnection();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to validate existing connection; reopening.", e);
            connection = openConnection();
        }
        return connection;
    }

    private static Connection openConnection() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException(
                    "db.properties not found on classpath. " +
                    "Place it in src/main/resources/.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load db.properties.", e);
        }

        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.username");
        String password = props.getProperty("db.password");

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            logger.info("Database connection established.");
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(
                "Cannot connect to the database. " +
                "Check db.properties and ensure MySQL is running.\n" + e.getMessage(), e);
        }
    }

    /** Closes the shared connection (call on application shutdown). */
    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed.");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing connection.", e);
            }
        }
    }
}
