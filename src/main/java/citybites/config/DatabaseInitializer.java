package citybites.config;

import citybites.util.ImageManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Creates all required tables and inserts default seed data on first run.
 * Safe to call on every application startup:
 *   - DDL uses IF NOT EXISTS
 *   - Seed inserts check for existing rows before inserting
 *   - One-time data migrations are tracked in the schema_migrations table
 *     and never executed more than once
 */
public class DatabaseInitializer {

    private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());

    /** Migration key for the one-time demo-stock initialisation. */
    private static final String MIG_DEMO_STOCK = "20260829_demo_stock_initialisation";

    /** Migration key for the one-time absolute image-path repair. */
    private static final String MIG_REPAIR_ABS_PATHS = "20260830_repair_absolute_image_paths";

    private DatabaseInitializer() {}

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            createTables(conn);
            applyMigrations(conn);
            seedAdmin(conn);
            seedFoodItems(conn);
            seedCustomer(conn);
            logger.info("Database initialised successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database initialisation failed.", e);
            throw new RuntimeException("Database initialisation failed: " + e.getMessage(), e);
        }
    }

    // ── Table Creation ────────────────────────────────────────────────────

    private static void createTables(Connection conn) throws SQLException {
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
                    food_id        INT AUTO_INCREMENT PRIMARY KEY,
                    food_name      VARCHAR(200)   NOT NULL,
                    price          DECIMAL(10,2)  NOT NULL,
                    available      TINYINT(1)     NOT NULL DEFAULT 1,
                    stock_quantity INT            NOT NULL DEFAULT 0,
                    image_path     VARCHAR(500)   DEFAULT NULL,
                    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id     INT AUTO_INCREMENT PRIMARY KEY,
                    customer_id  INT            NOT NULL,
                    order_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_amount DECIMAL(10,2)  NOT NULL,
                    status       ENUM('Pending','Preparing','Ready','Completed','Cancelled')
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

            // Tracks which one-time data migrations have been applied.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    migration_key VARCHAR(100) PRIMARY KEY,
                    applied_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
    }

    // ── Schema Migrations ────────────────────────────────────────────────

    private static void applyMigrations(Connection conn) throws SQLException {
        migrationAddStockColumn(conn);
        migrationAddReadyStatus(conn);
        migrationDemoStockInitialisation(conn);
        migrationRepairAbsoluteImagePaths(conn);
    }

    /** Migration 1: Add stock_quantity column if missing (pre-v1 databases). */
    private static void migrationAddStockColumn(Connection conn) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() " +
            "AND TABLE_NAME = 'food_items' " +
            "AND COLUMN_NAME = 'stock_quantity'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE food_items ADD COLUMN stock_quantity INT NOT NULL DEFAULT 0 " +
                    "AFTER available");
                logger.info("Migration applied: stock_quantity column added to food_items.");
            }
        }
    }

    /** Migration 2: Ensure 'Ready' value exists in orders.status ENUM. */
    private static void migrationAddReadyStatus(Connection conn) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() " +
            "AND TABLE_NAME = 'orders' " +
            "AND COLUMN_NAME = 'status' " +
            "AND COLUMN_TYPE LIKE '%Ready%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE orders MODIFY COLUMN status " +
                    "ENUM('Pending','Preparing','Ready','Completed','Cancelled') " +
                    "NOT NULL DEFAULT 'Pending'");
                logger.info("Migration applied: 'Ready' status added to orders.status ENUM.");
            }
        }
    }

    /**
     * Migration 3 (ONE-TIME): Sets realistic demo stock quantities for known menu
     * items whose stock_quantity is currently zero.
     *
     * <p>Behaviour guarantees:
     * <ul>
     *   <li>Runs <em>at most once</em> — tracked by the {@code schema_migrations} table.
     *   <li>Only updates items where {@code stock_quantity = 0} at the time of the
     *       first run; never overwrites positive stock.
     *   <li>On every subsequent startup the migration key is found in
     *       {@code schema_migrations} and the method returns immediately, so a food
     *       item whose stock has been legitimately ordered down to zero is never
     *       incorrectly reset to a non-zero value.
     *   <li>The UPDATE and the migration-key INSERT are executed in a single
     *       transaction so no partial state is possible.
     * </ul>
     */
    private static void migrationDemoStockInitialisation(Connection conn) throws SQLException {
        // Guard: already applied?
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_DEMO_STOCK);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;   // already applied — do nothing
        }

        Object[][] demoStock = {
            {25, "Chicken Fried Rice"},
            {20, "Chicken Kottu"},
            {30, "Cheese Burger"},
            {18, "Vegetable Sandwich"},
            {15, "Fish & Chips"},
            {20, "Vegetable Kottu"},
        };

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            int updated = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE food_items " +
                    "SET stock_quantity = ?, available = 1 " +
                    "WHERE LOWER(food_name) = LOWER(?) AND stock_quantity = 0")) {
                for (Object[] row : demoStock) {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String)  row[1]);
                    updated += ps.executeUpdate();
                }
            }

            // Record that this migration has been applied
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
                ins.setString(1, MIG_DEMO_STOCK);
                ins.executeUpdate();
            }

            conn.commit();
            logger.info(String.format(
                "Migration '%s' applied: %d food item(s) with stock=0 updated.",
                MIG_DEMO_STOCK, updated));
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    /**
     * Migration 4 (ONE-TIME): Repairs food_items rows whose image_path contains an
     * absolute file-system path (e.g. {@code C:\Users\...\photo.jpg}) by copying
     * the source file into the managed image directory and updating MySQL with the
     * managed relative filename.
     *
     * <p>Behaviour guarantees:
     * <ul>
     *   <li>Runs <em>at most once</em> — tracked by the {@code schema_migrations} table.</li>
     *   <li>Records that already use a managed relative filename are left unchanged.</li>
     *   <li>If the source file no longer exists, the row is logged but not modified;
     *       the UI will display the placeholder image.</li>
     *   <li>The original source file is never modified or deleted.</li>
     * </ul>
     */
    private static void migrationRepairAbsoluteImagePaths(Connection conn) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_REPAIR_ABS_PATHS);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;  // already applied — do nothing
        }

        int repaired = 0;
        try {
            repaired = repairAbsoluteImagePaths(conn);
        } catch (Exception e) {
            logger.log(Level.WARNING,
                "Image path repair encountered errors (partial repair may have occurred).", e);
        }

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
            ins.setString(1, MIG_REPAIR_ABS_PATHS);
            ins.executeUpdate();
        }
        logger.info(String.format(
            "Migration '%s' applied: %d absolute image path(s) repaired.",
            MIG_REPAIR_ABS_PATHS, repaired));
    }

    /**
     * Repairs food_items rows with absolute image paths by importing the source
     * file into the managed directory and updating MySQL with the relative filename.
     *
     * <p>This method is exposed {@code public} so that tests can invoke it directly
     * without going through the migration-tracking gate.
     *
     * @param conn an open database connection
     * @return number of rows successfully repaired
     * @throws Exception if a database error occurs during the repair
     */
    public static int repairAbsoluteImagePaths(Connection conn) throws Exception {
        // Collect rows with absolute image paths
        List<Integer> ids   = new ArrayList<>();
        List<String>  paths = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(
                 "SELECT food_id, image_path FROM food_items " +
                 "WHERE image_path IS NOT NULL AND image_path <> ''")) {
            while (rs.next()) {
                String imgPath = rs.getString("image_path");
                if (isAbsolutePath(imgPath)) {
                    ids.add(rs.getInt("food_id"));
                    paths.add(imgPath);
                }
            }
        }

        int repaired = 0;
        for (int i = 0; i < ids.size(); i++) {
            int    foodId  = ids.get(i);
            String absPath = paths.get(i);
            try {
                java.nio.file.Path src = java.nio.file.Path.of(absPath);
                String managed = ImageManager.importImage(src);
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE food_items SET image_path = ? WHERE food_id = ?")) {
                    upd.setString(1, managed);
                    upd.setInt(2, foodId);
                    upd.executeUpdate();
                }
                logger.info(String.format(
                    "Repaired image_path for food_id=%d: '%s' → '%s'",
                    foodId, absPath, managed));
                repaired++;
            } catch (Exception ex) {
                // Source file missing or import failed — log and leave the row unchanged
                // so the UI shows a placeholder rather than crashing.
                logger.warning(String.format(
                    "Could not repair image_path for food_id=%d ('%s'): %s",
                    foodId, absPath, ex.getMessage()));
            }
        }
        return repaired;
    }

    /**
     * Returns {@code true} when {@code path} looks like a Windows or Unix absolute path.
     * Windows: starts with a drive letter followed by {@code :\} or {@code :/}.
     * Unix / Git-Bash: starts with {@code /}.
     */
    private static boolean isAbsolutePath(String path) {
        if (path == null) return false;
        return path.matches("^[A-Za-z]:[/\\\\].*") || path.startsWith("/");
    }

    // ── Seed Data ────────────────────────────────────────────────────────

    private static void seedAdmin(Connection conn) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM admins WHERE username = ?")) {
            check.setString(1, "admin");
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO admins (username, password_hash) VALUES (?, ?)")) {
            ps.setString(1, "admin");
            ps.setString(2, hash);
            ps.executeUpdate();
            logger.info("Default admin seeded (username=admin).");
        }
    }

    /**
     * Seeds the initial menu. Only runs when the food_items table is empty.
     * Stock quantities are included here so that a fresh database has correct
     * inventory without requiring the one-time migration.
     */
    private static void seedFoodItems(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM food_items")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO food_items (food_name, price, available, stock_quantity) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] items = {
                {"Chicken Fried Rice",  850.00, true, 25},
                {"Chicken Kottu",       750.00, true, 20},
                {"Cheese Burger",       650.00, true, 30},
                {"Vegetable Sandwich",  450.00, true, 18},
                {"Fish & Chips",        950.00, true, 15},
            };
            for (Object[] item : items) {
                ps.setString(1,  (String)  item[0]);
                ps.setDouble(2,  (Double)  item[1]);
                ps.setBoolean(3, (Boolean) item[2]);
                ps.setInt(4,     (Integer) item[3]);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Sample food items seeded with stock quantities.");
        }
    }

    /**
     * Seeds the demo customer account only on a fresh database.
     * Password 'Demo1234' satisfies the registration policy (8+ chars, letter + digit).
     * Existing BCrypt hashes already in the database are never modified.
     */
    private static void seedCustomer(Connection conn) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM customers WHERE username = ?")) {
            check.setString(1, "customer");
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String hash = BCrypt.hashpw("Demo1234", BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customers (full_name, username, password_hash) VALUES (?, ?, ?)")) {
            ps.setString(1, "Sample Customer");
            ps.setString(2, "customer");
            ps.setString(3, hash);
            ps.executeUpdate();
            logger.info("Sample customer seeded (username=customer, password=Demo1234).");
        }
    }
}
