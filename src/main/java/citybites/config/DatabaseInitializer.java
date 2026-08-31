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

    /** Migration key for adding category_id FK column to food_items. */
    private static final String MIG_ADD_CATEGORY_ID = "20260830_add_category_id_to_food_items";

    /** Migration key for enforcing category_id NOT NULL + ON DELETE RESTRICT on food_items. */
    private static final String MIG_ENFORCE_CATEGORY_REQUIRED =
            "20260830_enforce_food_category_required";

    /** Migration key for adding description column to food_categories. */
    private static final String MIG_ADD_CATEGORY_DESCRIPTION =
            "20260831_add_food_category_description";

    /** Migration key for changing orders.customer_id FK from CASCADE to RESTRICT. */
    private static final String MIG_ORDERS_CUSTOMER_FK_RESTRICT =
            "20260831_orders_customer_fk_restrict";

    private DatabaseInitializer() {}

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            createTables(conn);
            seedAdmin(conn);
            // seedFoodCategories must run before applyMigrations so that the
            // "Other" category exists when migrationEnforceCategoryRequired executes.
            seedFoodCategories(conn);
            applyMigrations(conn);
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
                CREATE TABLE IF NOT EXISTS food_categories (
                    category_id   INT AUTO_INCREMENT PRIMARY KEY,
                    category_name VARCHAR(100) NOT NULL UNIQUE,
                    description   VARCHAR(255) NULL,
                    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

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
                    category_id    INT            NOT NULL,
                    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_food_items_cat FOREIGN KEY (category_id)
                        REFERENCES food_categories(category_id) ON DELETE RESTRICT
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
                    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id) ON DELETE RESTRICT
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
        migrationAddCategoryIdColumn(conn);
        migrationEnforceCategoryRequired(conn);
        migrationAddCategoryDescription(conn);
        migrationOrdersCustomerFkRestrict(conn);
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

    /**
     * Migration 5: Adds the category_id nullable FK column to food_items.
     * Runs at most once; tracked by schema_migrations.
     * food_categories table must already exist before this migration runs.
     */
    private static void migrationAddCategoryIdColumn(Connection conn) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_ADD_CATEGORY_ID);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;   // already applied
        }

        // Check whether column already exists (handles manual intervention or fresh-install DDL)
        String colCheck =
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() " +
            "AND TABLE_NAME = 'food_items' " +
            "AND COLUMN_NAME = 'category_id'";
        try (PreparedStatement ps = conn.prepareStatement(colCheck);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE food_items ADD COLUMN category_id INT NULL " +
                    "AFTER image_path");
                conn.createStatement().execute(
                    "ALTER TABLE food_items ADD CONSTRAINT fk_food_items_category " +
                    "FOREIGN KEY (category_id) REFERENCES food_categories(category_id) " +
                    "ON DELETE SET NULL");
                logger.info("Migration applied: category_id column + FK added to food_items.");
            }
        }

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
            ins.setString(1, MIG_ADD_CATEGORY_ID);
            ins.executeUpdate();
        }
    }

    /**
     * Migration 6 (ONE-TIME): Enforces category_id NOT NULL + ON DELETE RESTRICT on food_items.
     *
     * <p>Steps:
     * <ol>
     *   <li>Ensure the "Other" fall-back category exists (INSERT IGNORE).</li>
     *   <li>Assign "Other" to every food_items row whose category_id is NULL.</li>
     *   <li>Assign "Other" to every food_items row whose category_id references a
     *       non-existent category (orphan repair).</li>
     *   <li>If category_id is still nullable (IS_NULLABLE = 'YES'):
     *     <ul>
     *       <li>Discover the existing FK constraint name from information_schema.</li>
     *       <li>DROP the old FK (which had ON DELETE SET NULL).</li>
     *       <li>MODIFY the column to INT NOT NULL.</li>
     *       <li>ADD a new FK with ON DELETE RESTRICT.</li>
     *     </ul>
     *   </li>
     *   <li>Record the migration key (idempotent guard on future startups).</li>
     * </ol>
     *
     * <p>Note: ALTER TABLE causes an implicit commit in MySQL; full transactional
     * rollback is not possible for DDL. The data-only UPDATEs (steps 2–3) are safe
     * to re-run because step 1 re-checks IS_NULLABLE before performing any DDL.
     */
    private static void migrationEnforceCategoryRequired(Connection conn) throws SQLException {
        // Guard: already applied?
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_ENFORCE_CATEGORY_REQUIRED);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        // Step 1: Ensure "Other" category exists (idempotent)
        conn.createStatement().execute(
            "INSERT IGNORE INTO food_categories (category_name) VALUES ('Other')");

        // Step 2: Resolve "Other" ID (never hardcoded — always fetched from DB)
        int otherId;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT category_id FROM food_categories WHERE category_name = 'Other'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("'Other' category not found after INSERT IGNORE.");
            }
            otherId = rs.getInt(1);
        }

        // Step 3: Assign "Other" to uncategorised rows
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE food_items SET category_id = ? WHERE category_id IS NULL")) {
            ps.setInt(1, otherId);
            int n = ps.executeUpdate();
            if (n > 0) logger.info(String.format(
                "Migration '%s': assigned 'Other' to %d uncategorised food item(s).",
                MIG_ENFORCE_CATEGORY_REQUIRED, n));
        }

        // Step 4: Assign "Other" to rows with orphaned (non-existent) category references
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE food_items SET category_id = ? " +
                "WHERE category_id NOT IN (SELECT category_id FROM food_categories)")) {
            ps.setInt(1, otherId);
            int n = ps.executeUpdate();
            if (n > 0) logger.info(String.format(
                "Migration '%s': reassigned 'Other' to %d orphaned food item(s).",
                MIG_ENFORCE_CATEGORY_REQUIRED, n));
        }

        // Step 5: Check whether DDL change is still needed
        String isNullable;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'food_items' " +
                "AND COLUMN_NAME = 'category_id'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("category_id column not found in information_schema.");
            }
            isNullable = rs.getString(1);
        }

        if ("YES".equals(isNullable)) {
            // Step 6: Discover the existing FK constraint name (do NOT hardcode it)
            String fkName = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'food_items' " +
                    "AND COLUMN_NAME = 'category_id' " +
                    "AND REFERENCED_TABLE_NAME = 'food_categories'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) fkName = rs.getString(1);
            }

            // Step 7: Drop old FK (if it exists)
            if (fkName != null) {
                conn.createStatement().execute(
                    "ALTER TABLE food_items DROP FOREIGN KEY " + fkName);
                logger.info("Migration: dropped FK '" + fkName + "' (ON DELETE SET NULL).");
            }

            // Step 8: Change column to NOT NULL
            conn.createStatement().execute(
                "ALTER TABLE food_items MODIFY COLUMN category_id INT NOT NULL");

            // Step 9: Recreate FK with ON DELETE RESTRICT
            conn.createStatement().execute(
                "ALTER TABLE food_items ADD CONSTRAINT fk_food_items_cat " +
                "FOREIGN KEY (category_id) REFERENCES food_categories(category_id) " +
                "ON DELETE RESTRICT");

            logger.info(String.format(
                "Migration '%s': category_id enforced NOT NULL + ON DELETE RESTRICT.",
                MIG_ENFORCE_CATEGORY_REQUIRED));
        } else {
            logger.info(String.format(
                "Migration '%s': category_id already NOT NULL — DDL skipped.",
                MIG_ENFORCE_CATEGORY_REQUIRED));
        }

        // Step 10: Record migration key
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
            ins.setString(1, MIG_ENFORCE_CATEGORY_REQUIRED);
            ins.executeUpdate();
        }
    }

    /**
     * Migration 7 (ONE-TIME): Adds the optional {@code description VARCHAR(255) NULL}
     * column to {@code food_categories}.
     *
     * <p>Idempotency is doubly guarded:
     * <ul>
     *   <li>The migration key in {@code schema_migrations} prevents a second execution.</li>
     *   <li>The {@code information_schema} column check prevents a duplicate ALTER TABLE
     *       even if the key was somehow cleared (e.g. manual DB restore).</li>
     * </ul>
     * Existing category rows are not affected — {@code description} defaults to NULL.
     */
    private static void migrationAddCategoryDescription(Connection conn) throws SQLException {
        // Guard: already applied?
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_ADD_CATEGORY_DESCRIPTION);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        // Check whether column already exists (handles fresh-install DDL that already has it)
        String colCheck =
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() " +
            "AND TABLE_NAME = 'food_categories' " +
            "AND COLUMN_NAME = 'description'";
        try (PreparedStatement ps = conn.prepareStatement(colCheck);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().execute(
                    "ALTER TABLE food_categories " +
                    "ADD COLUMN description VARCHAR(255) NULL AFTER category_name");
                logger.info("Migration applied: description column added to food_categories.");
            } else {
                logger.info("Migration '" + MIG_ADD_CATEGORY_DESCRIPTION +
                            "': description column already exists — DDL skipped.");
            }
        }

        // Record migration key
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
            ins.setString(1, MIG_ADD_CATEGORY_DESCRIPTION);
            ins.executeUpdate();
        }
    }

    /**
     * Migration 8 (ONE-TIME): Changes the {@code orders.customer_id} foreign key
     * from {@code ON DELETE CASCADE} to {@code ON DELETE RESTRICT}.
     *
     * <p>Rationale: CASCADE silently deletes all order history when a customer is
     * removed, making accidental data loss impossible to recover. RESTRICT forces
     * the admin UI to explicitly acknowledge orders before deletion can proceed.
     *
     * <p>Steps:
     * <ol>
     *   <li>Look up the current DELETE_RULE for the FK in
     *       {@code information_schema.REFERENTIAL_CONSTRAINTS}.
     *   <li>If it is already {@code RESTRICT} or {@code NO ACTION}, record the key
     *       and return — no DDL needed.</li>
     *   <li>Otherwise discover the FK constraint name, DROP it, and re-ADD it with
     *       {@code ON DELETE RESTRICT}.</li>
     *   <li>Record the migration key (idempotent guard on future startups).</li>
     * </ol>
     */
    private static void migrationOrdersCustomerFkRestrict(Connection conn) throws SQLException {
        // Guard: already applied?
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            check.setString(1, MIG_ORDERS_CUSTOMER_FK_RESTRICT);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        // Check current DELETE_RULE for orders → customers FK
        String deleteRule = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DELETE_RULE " +
                "FROM information_schema.REFERENTIAL_CONSTRAINTS " +
                "WHERE CONSTRAINT_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'orders' " +
                "AND REFERENCED_TABLE_NAME = 'customers'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) deleteRule = rs.getString(1);
        }

        if (deleteRule == null) {
            logger.warning("Migration '" + MIG_ORDERS_CUSTOMER_FK_RESTRICT +
                "': no FK found on orders.customer_id — skipping DDL.");
        } else if ("RESTRICT".equals(deleteRule) || "NO ACTION".equals(deleteRule)) {
            logger.info("Migration '" + MIG_ORDERS_CUSTOMER_FK_RESTRICT +
                "': orders.customer_id FK is already RESTRICT — DDL skipped.");
        } else {
            // Discover the constraint name
            String fkName = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONSTRAINT_NAME " +
                    "FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'orders' " +
                    "AND COLUMN_NAME = 'customer_id' " +
                    "AND REFERENCED_TABLE_NAME = 'customers'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) fkName = rs.getString(1);
            }
            if (fkName != null) {
                conn.createStatement().execute(
                    "ALTER TABLE orders DROP FOREIGN KEY " + fkName);
                logger.info("Migration: dropped FK '" + fkName +
                    "' (ON DELETE " + deleteRule + ") from orders.customer_id.");
            }
            conn.createStatement().execute(
                "ALTER TABLE orders ADD CONSTRAINT fk_orders_customer " +
                "FOREIGN KEY (customer_id) REFERENCES customers(customer_id) " +
                "ON DELETE RESTRICT");
            logger.info("Migration '" + MIG_ORDERS_CUSTOMER_FK_RESTRICT +
                "': orders.customer_id FK changed to ON DELETE RESTRICT.");
        }

        // Record migration key
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_migrations (migration_key) VALUES (?)")) {
            ins.setString(1, MIG_ORDERS_CUSTOMER_FK_RESTRICT);
            ins.executeUpdate();
        }
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
     * Seeds default food categories on a fresh database (when food_categories is empty).
     * "Other" is always listed first so that it is the mandatory fallback category
     * available before any migrations run.
     */
    private static void seedFoodCategories(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM food_categories")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO food_categories (category_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String name : new String[]{
                    "Other", "Rice Dishes", "Kottu", "Burgers", "Sandwiches",
                    "Seafood", "Beverages", "Desserts"}) {
                ps.setString(1, name);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Default food categories seeded (including 'Other' fallback).");
        }
    }

    /**
     * Returns the category_id for the given category name, or -1 if not found.
     * Used by seedFoodItems to resolve category IDs without hardcoding them.
     */
    private static int getCategoryIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT category_id FROM food_categories WHERE category_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Seeds the initial menu. Only runs when the food_items table is empty.
     * Stock quantities and category_id values are included so that a fresh
     * database has correct inventory and categorisation without any migration.
     * Category IDs are resolved by name — never hardcoded.
     */
    private static void seedFoodItems(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM food_items")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        // Resolve category IDs by name; fall back to "Other" if a category is missing.
        int riceId     = getCategoryIdByName(conn, "Rice Dishes");
        int kottuId    = getCategoryIdByName(conn, "Kottu");
        int burgersId  = getCategoryIdByName(conn, "Burgers");
        int sandwichId = getCategoryIdByName(conn, "Sandwiches");
        int seafoodId  = getCategoryIdByName(conn, "Seafood");
        int otherId    = getCategoryIdByName(conn, "Other");

        // If a named category is missing (should not happen on fresh install), use "Other"
        if (riceId     < 0) riceId     = otherId;
        if (kottuId    < 0) kottuId    = otherId;
        if (burgersId  < 0) burgersId  = otherId;
        if (sandwichId < 0) sandwichId = otherId;
        if (seafoodId  < 0) seafoodId  = otherId;

        String sql =
            "INSERT INTO food_items (food_name, price, available, stock_quantity, category_id) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] items = {
                {"Chicken Fried Rice",  850.00, true, 25, riceId},
                {"Chicken Kottu",       750.00, true, 20, kottuId},
                {"Cheese Burger",       650.00, true, 30, burgersId},
                {"Vegetable Sandwich",  450.00, true, 18, sandwichId},
                {"Fish & Chips",        950.00, true, 15, seafoodId},
            };
            for (Object[] item : items) {
                ps.setString(1,  (String)  item[0]);
                ps.setDouble(2,  (Double)  item[1]);
                ps.setBoolean(3, (Boolean) item[2]);
                ps.setInt(4,     (Integer) item[3]);
                ps.setInt(5,     (Integer) item[4]);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Sample food items seeded with stock quantities and categories.");
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
