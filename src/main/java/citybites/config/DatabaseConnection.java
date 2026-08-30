package citybites.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Provides a new JDBC connection on each call to {@link #getConnection()}.
 * Callers must close the connection — use try-with-resources.
 */
public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "db.properties not found on classpath. " +
                        "Copy db.properties.example to db.properties and fill in credentials.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load db.properties.", e);
        }
        URL      = props.getProperty("db.url");
        USER     = props.getProperty("db.username");
        PASSWORD = props.getProperty("db.password");
        logger.info("Database credentials loaded.");
    }

    private DatabaseConnection() {}

    /**
     * Opens and returns a new JDBC connection.
     * The caller is responsible for closing it — use try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
