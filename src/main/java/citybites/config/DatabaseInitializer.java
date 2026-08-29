package citybites.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Creates all required tables and inserts default seed data
 * (admin account + sample food items + sample customer) on first run.
 * Safe to call on every startup — uses IF NOT EXISTS / INSERT IGNORE.
 */
public class DatabaseInitializer {

    private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());

    private DatabaseInitializer() {}

    public static void initialize() {
        try {
            createTables();
            seedAdmin();
            seedFoodItems();
            seedCustomer();
            logger.info("Database initialised successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database initialisation failed.", e);
            throw new RuntimeException("Database initialisation failed: " + e.getMessage(), e);
        }
    }

    // ── Table Creation ───────────────────────────────────────────────────────

    private static void createTables() throws SQLException {
        Connection conn = DatabaseConnection.get();
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    admin_id      INT AUTO_INCREMENT PRIMARY KEY,
                    username      VARCHAR(50)  NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id   INT AUTO_INCREMENT PRIMARY KEY,
                    full_name     VARCHAR(100) NOT NULL,
                    username      VARCHAR(50)  NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS food_items (
                    food_id    INT AUTO_INCREMENT PRIMARY KEY,
                    food_name  VARCHAR(200)   NOT NULL,
                    price      DECIMAL(10,2)  NOT NULL,
                    available  TINYINT(1)     NOT NULL DEFAULT 1,
                    image_path VARCHAR(500)   DEFAULT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id     INT AUTO_INCREMENT PRIMARY KEY,
                    customer_id  INT            NOT NULL,
                    order_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_amount DECIMAL(10,2)  NOT NULL,
                    status       ENUM('Pending','Preparing','Completed','Cancelled')
                                 NOT NULL DEFAULT 'Pending',
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                        ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    item_id    INT AUTO_INCREMENT PRIMARY KEY,
                    order_id   INT            NOT NULL,
                    food_id    INT            NOT NULL,
                    food_name  VARCHAR(200)   NOT NULL,
                    unit_price DECIMAL(10,2)  NOT NULL,
                    quantity   INT            NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (food_id)  REFERENCES food_items(food_id)
                        ON DELETE RESTRICT
                )
                """);
        }
    }

    // ── Seed Data ────────────────────────────────────────────────────────────

    private static void seedAdmin() throws SQLException {
        Connection conn = DatabaseConnection.get();
        String sql = "SELECT COUNT(*) FROM admins WHERE username = ?";
        try (PreparedStatement check = conn.prepareStatement(sql)) {
            check.setString(1, "admin");
            try (ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return;   // already seeded
            }
        }
        String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO admins (username, password_hash) VALUES (?, ?)")) {
            ps.setString(1, "admin");
            ps.setString(2, hash);
            ps.executeUpdate();
            logger.info("Default admin account created (username=admin, password=admin123).");
        }
    }

    private static void seedFoodItems() throws SQLException {
        Connection conn = DatabaseConnection.get();
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM food_items")) {
            rs.next();
            if (rs.getInt(1) > 0) return;   // already seeded
        }
        String sql = "INSERT INTO food_items (food_name, price, available) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] items = {
                {"Chicken Fried Rice",  850.00, true},
                {"Chicken Kottu",       750.00, true},
                {"Cheese Burger",       650.00, true},
                {"Vegetable Sandwich",  450.00, true},
                {"Fish & Chips",        950.00, true},
            };
            for (Object[] item : items) {
                ps.setString(1, (String) item[0]);
                ps.setDouble(2, (Double) item[1]);
                ps.setBoolean(3, (Boolean) item[2]);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Sample food items inserted.");
        }
    }

    private static void seedCustomer() throws SQLException {
        Connection conn = DatabaseConnection.get();
        String check = "SELECT COUNT(*) FROM customers WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, "customer");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return;   // already seeded
            }
        }
        String hash = BCrypt.hashpw("1234", BCrypt.gensalt());
        String sql  = "INSERT INTO customers (full_name, username, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Sample Customer");
            ps.setString(2, "customer");
            ps.setString(3, hash);
            ps.executeUpdate();
            logger.info("Sample customer account created (username=customer, password=1234).");
        }
    }
}
