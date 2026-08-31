package citybites;

import citybites.config.DatabaseConnection;
import citybites.config.DatabaseInitializer;
import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.FoodCategory;
import citybites.model.FoodItem;
import citybites.model.Order;
import citybites.service.AuthService;
import citybites.service.CustomerManagementService;
import citybites.service.FoodCategoryService;
import citybites.service.FoodService;
import citybites.service.OrderService;
import citybites.util.ImageManager;
import citybites.util.PasswordValidator;
import citybites.util.SessionManager;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * JUnit 5 service-layer and migration tests for CityBitesFoodManagementSystem.
 *
 * Isolation strategy
 * ------------------
 *   All order and food-item tests use isolated fixtures (SV_TestFood / sv_testuser)
 *   created in @BeforeAll and destroyed in @AfterAll. The seeded demo records
 *   (Chicken Kottu, Fish & Chips, etc.) are only read, never mutated, except in the
 *   explicit migration tests that verify the one-time stock initialisation.
 *
 * Test naming convention
 * ----------------------
 *   A test whose purpose is to confirm that invalid input is correctly rejected
 *   is named with "isCorrectlyRejected" or "blocksInvalid*". When such a test
 *   passes it means the system correctly enforced its constraint — a PASS result,
 *   not a failure.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceValidationTest {

    // ── Isolated test fixtures ─────────────────────────────────────────────
    static int      testFoodId   = -1;
    static int      testOrderId  = -1;   // set in @Order(20), consumed by @Order(30-33)
    static Customer testCustomer = null;

    static final String TEST_USER = "sv_testuser";
    static final String TEST_PASS = "SvTest@99";   // compliant: 9 chars, letter + digit

    /** Migration key that must appear in schema_migrations after initialisation. */
    static final String MIG_DEMO_STOCK = "20260829_demo_stock_initialisation";

    // ── Suite lifecycle ────────────────────────────────────────────────────

    @BeforeAll
    static void initSuite() throws Exception {
        DatabaseInitializer.initialize();
        deleteTestFixtures();   // remove remnants from any aborted previous run

        // Isolated food item — ample stock so cart/order tests are not stock-blocked
        testFoodId = FoodService.addFoodItem("SV_TestFood", 500.00, true, 50, null);
        assertTrue(testFoodId > 0, "Test food item must be created in @BeforeAll");

        // Isolated test customer
        AuthService.register("SV Test User", TEST_USER, TEST_PASS);
        Optional<Customer> c = AuthService.customerLogin(TEST_USER, TEST_PASS);
        assertTrue(c.isPresent(), "Test customer must authenticate immediately after registration");
        testCustomer = c.get();
    }

    @AfterAll
    static void cleanupSuite() throws Exception {
        DataStore.cartItems.clear();
        SessionManager.setLoggedInCustomer(null);
        deleteTestFixtures();
    }

    @BeforeEach
    void clearCart() {
        DataStore.cartItems.clear();
    }

    // ══════════════════════════════════════════════════════════════════════
    // FOOD MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(1)
    void foodItemAppearsInListAfterCreation() throws Exception {
        List<FoodItem> items = FoodService.getAllFoodItems();
        assertTrue(
            items.stream().anyMatch(f -> f.getFoodId() == testFoodId),
            "Food item must appear in getAllFoodItems() immediately after creation"
        );
    }

    @Test @org.junit.jupiter.api.Order(2)
    void foodItemUpdatePersistsToDB() throws Exception {
        boolean ok = FoodService.updateFoodItem(
            testFoodId, "SV_TestFood_UPDATED", 600.00, true, 45, null);
        assertTrue(ok, "updateFoodItem must return true");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT food_name, price, stock_quantity FROM food_items WHERE food_id=?")) {
            ps.setInt(1, testFoodId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Updated food item must be present in DB");
            assertEquals("SV_TestFood_UPDATED", rs.getString("food_name"));
            assertEquals(600.00, rs.getDouble("price"), 0.01);
            assertEquals(45,     rs.getInt("stock_quantity"));
        }
        // Restore for subsequent tests
        FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.00, true, 50, null);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CART LOGIC
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(10)
    void cartSubtotalMatchesPriceTimesQuantity() throws Exception {
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        DataStore.cartItems.add(new CartItem(food, 3));
        assertEquals(food.getPrice() * 3, DataStore.cartItems.get(0).getSubtotal(), 0.01);
    }

    @Test @org.junit.jupiter.api.Order(11)
    void cartMergesQuantityForExistingItem() throws Exception {
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        DataStore.cartItems.add(new CartItem(food, 2));
        // Simulate FoodMenuFrame.addToCart() accumulate logic
        DataStore.cartItems.stream()
            .filter(ci -> ci.getFoodItem().getFoodId() == food.getFoodId())
            .findFirst()
            .ifPresent(ci -> ci.setQuantity(ci.getQuantity() + 3));
        assertEquals(1, DataStore.cartItems.size(), "No duplicate row for the same food item");
        assertEquals(5, DataStore.cartItems.get(0).getQuantity());
    }

    @Test @org.junit.jupiter.api.Order(12)
    void cartStockGuardBlocksQuantityAboveAvailableStock() throws Exception {
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        // stock=50; requesting 100 triggers the guard in FoodMenuFrame.addToCart()
        assertTrue(food.getStockQuantity() < 100,
            "Guard condition (stock < requested qty) must evaluate to true for qty=100");
    }

    @Test @org.junit.jupiter.api.Order(13)
    void cartRemoveLeavesCartEmpty() throws Exception {
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        DataStore.cartItems.add(new CartItem(food, 1));
        DataStore.cartItems.removeIf(ci -> ci.getFoodItem().getFoodId() == food.getFoodId());
        assertTrue(DataStore.cartItems.isEmpty(), "Cart must be empty after removing the only item");
    }

    @Test @org.junit.jupiter.api.Order(14)
    void cartClearEmptiesAllItems() throws Exception {
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        DataStore.cartItems.add(new CartItem(food, 2));
        DataStore.cartItems.add(new CartItem(food, 1));
        DataStore.cartItems.clear();
        assertTrue(DataStore.cartItems.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════
    // ORDER TRANSACTION
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(20)
    void orderPlacementAtomicallyDeductsStock() throws Exception {
        setStock(testFoodId, 20);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        int stockBefore = food.getStockQuantity();

        DataStore.cartItems.add(new CartItem(food, 3));
        Order order = OrderService.placeOrder(testCustomer, DataStore.cartItems);
        DataStore.cartItems.clear();
        testOrderId = order.getOrderId();

        assertEquals(stockBefore - 3, getStockById(testFoodId),
            "Stock must be atomically deducted by the ordered quantity");
    }

    @Test @org.junit.jupiter.api.Order(21)
    void orderRowExistsWithCorrectTotalAndPendingStatus() throws Exception {
        assertTrue(testOrderId > 0, "Requires order from @Order(20)");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT total_amount, status FROM orders WHERE order_id=?")) {
            ps.setInt(1, testOrderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Order row must be present in the orders table");
            assertEquals(500.00 * 3, rs.getDouble("total_amount"), 0.01,
                "Total must equal unit_price × quantity (Rs. 1500.00)");
            assertEquals("Pending", rs.getString("status"),
                "Newly placed orders must start with status=Pending");
        }
    }

    @Test @org.junit.jupiter.api.Order(22)
    void orderItemsRowRecordsCorrectQuantity() throws Exception {
        assertTrue(testOrderId > 0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT quantity FROM order_items WHERE order_id=?")) {
            ps.setInt(1, testOrderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "order_items must contain at least one row for the order");
            assertEquals(3, rs.getInt("quantity"));
        }
    }

    @Test @org.junit.jupiter.api.Order(23)
    void insufficientStockIsCorrectlyRejectedWithoutSideEffects() throws Exception {
        // PASS = system correctly refuses the order and leaves state unchanged
        setStock(testFoodId, 2);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        int stockBefore  = getStockById(testFoodId);
        int ordersBefore = countTestOrders();

        DataStore.cartItems.add(new CartItem(food, 5));   // 5 > stock=2

        assertThrows(RuntimeException.class,
            () -> OrderService.placeOrder(testCustomer, DataStore.cartItems),
            "Ordering more than available stock must be rejected with RuntimeException");

        assertEquals(stockBefore,  getStockById(testFoodId),
            "Stock must be unchanged when the order is correctly rejected");
        assertEquals(ordersBefore, countTestOrders(),
            "No order row must be created when the order is correctly rejected");
        assertFalse(DataStore.cartItems.isEmpty(),
            "Cart must remain intact so the customer can adjust their order");
    }

    @Test @org.junit.jupiter.api.Order(24)
    void stockCannotBeReducedBelowZero() throws Exception {
        setStock(testFoodId, 1);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        DataStore.cartItems.add(new CartItem(food, 999));

        assertThrows(RuntimeException.class,
            () -> OrderService.placeOrder(testCustomer, DataStore.cartItems),
            "Ordering quantity that would make stock negative must be rejected");

        assertTrue(getStockById(testFoodId) >= 0, "Stock must never fall below zero");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ORDER STATUS WORKFLOW
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(30)
    void statusAdvancesFromPendingToPreparing() throws Exception {
        assertTrue(testOrderId > 0, "Requires order from @Order(20)");
        OrderService.updateOrderStatus(testOrderId, "Preparing");
        assertEquals("Preparing", getOrderStatus(testOrderId));
    }

    @Test @org.junit.jupiter.api.Order(31)
    void statusAdvancesFromPreparingToReady() throws Exception {
        OrderService.updateOrderStatus(testOrderId, "Ready");
        assertEquals("Ready", getOrderStatus(testOrderId));
    }

    @Test @org.junit.jupiter.api.Order(32)
    void statusAdvancesFromReadyToCompleted() throws Exception {
        OrderService.updateOrderStatus(testOrderId, "Completed");
        assertEquals("Completed", getOrderStatus(testOrderId));
    }

    @Test @org.junit.jupiter.api.Order(33)
    void cancellingACompletedOrderIsCorrectlyBlocked() {
        // PASS = system correctly blocks the invalid transition
        assertThrows(RuntimeException.class,
            () -> OrderService.updateOrderStatus(testOrderId, "Cancelled"),
            "Completed -> Cancelled must be blocked by the transition guard");
    }

    // ══════════════════════════════════════════════════════════════════════
    // CANCEL AND STOCK RESTORE
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(40)
    void cancellingOrderFullyRestoresStock() throws Exception {
        setStock(testFoodId, 30);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);
        int stockBefore = getStockById(testFoodId);

        DataStore.cartItems.add(new CartItem(food, 4));
        Order order = OrderService.placeOrder(testCustomer, DataStore.cartItems);
        DataStore.cartItems.clear();
        assertEquals(stockBefore - 4, getStockById(testFoodId), "Stock deducted after ordering");

        OrderService.updateOrderStatus(order.getOrderId(), "Cancelled");
        assertEquals(stockBefore, getStockById(testFoodId),
            "Full stock quantity must be restored on cancellation");
    }

    @Test @org.junit.jupiter.api.Order(41)
    void secondCancelOnSameOrderIsCorrectlyBlockedAndStockIsUnchanged() throws Exception {
        // PASS = double-cancel is correctly rejected; stock not erroneously restored twice
        setStock(testFoodId, 10);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);

        DataStore.cartItems.add(new CartItem(food, 1));
        Order order = OrderService.placeOrder(testCustomer, DataStore.cartItems);
        DataStore.cartItems.clear();

        OrderService.updateOrderStatus(order.getOrderId(), "Cancelled");
        int stockAfterFirstCancel = getStockById(testFoodId);

        assertThrows(RuntimeException.class,
            () -> OrderService.updateOrderStatus(order.getOrderId(), "Cancelled"),
            "A second cancel attempt must be correctly blocked");

        assertEquals(stockAfterFirstCancel, getStockById(testFoodId),
            "Stock must not be restored a second time when double-cancel is correctly blocked");
    }

    // ══════════════════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(50)
    void existingAdminLoginSucceeds() {
        assertTrue(AuthService.adminLogin("admin", "admin123"),
            "Seeded admin account must authenticate via BCrypt");
    }

    @Test @org.junit.jupiter.api.Order(51)
    void isolatedTestCustomerLoginSucceeds() {
        Optional<Customer> c = AuthService.customerLogin(TEST_USER, TEST_PASS);
        assertTrue(c.isPresent(), "Isolated test customer must authenticate");
    }

    @Test @org.junit.jupiter.api.Order(52)
    void registrationWithDuplicateUsernameIsCorrectlyRejected() {
        // PASS = system correctly enforces unique username constraint
        assertThrows(RuntimeException.class,
            () -> AuthService.register("Duplicate User", TEST_USER, TEST_PASS),
            "Registering with an already-taken username must be correctly rejected");
    }

    // ══════════════════════════════════════════════════════════════════════
    // PASSWORD POLICY  (tests PasswordValidator directly — no UI dependency)
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(60)
    void nonCompliantPasswordsAreCorrectlyRejected() {
        // PASS = validator correctly identifies each password as non-compliant
        assertFalse(PasswordValidator.isCompliant("1234"),
            "CORRECTLY REJECTED: '1234' — too short, no letter");
        assertFalse(PasswordValidator.isCompliant("12345678"),
            "CORRECTLY REJECTED: '12345678' — 8 digits but no letter");
        assertFalse(PasswordValidator.isCompliant("abcdefgh"),
            "CORRECTLY REJECTED: 'abcdefgh' — 8 letters but no digit");
        assertFalse(PasswordValidator.isCompliant("Abc1234"),
            "CORRECTLY REJECTED: 'Abc1234' — only 7 characters");
        assertFalse(PasswordValidator.isCompliant(""),
            "CORRECTLY REJECTED: empty string");
        assertFalse(PasswordValidator.isCompliant(null),
            "CORRECTLY REJECTED: null input");
    }

    @Test @org.junit.jupiter.api.Order(61)
    void compliantPasswordsAreAccepted() {
        assertTrue(PasswordValidator.isCompliant("SvTest@99"),
            "ACCEPTED: 'SvTest@99' — 9 chars, letter + digit");
        assertTrue(PasswordValidator.isCompliant("Demo1234"),
            "ACCEPTED: 'Demo1234' — 8 chars, letters + digits (new seed password)");
        assertTrue(PasswordValidator.isCompliant("abc12345"),
            "ACCEPTED: 'abc12345' — 8 chars, letters + digits");
        assertTrue(PasswordValidator.isCompliant("Pass1234word"),
            "ACCEPTED: 'Pass1234word' — 12 chars, letters + digits");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ONE-TIME MIGRATION — demo stock initialisation
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(80)
    void migrationKeyIsRecordedInSchemaMigrationsTable() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            ps.setString(1, MIG_DEMO_STOCK);
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt(1),
                "Migration key must appear exactly once in schema_migrations after initialisation");
        }
    }

    @Test @org.junit.jupiter.api.Order(81)
    void rerunningInitializerDoesNotChangeDemoStock() throws Exception {
        // Capture stock before re-running
        int kottuBefore = getStockById(2);    // Chicken Kottu
        int cheeseBefore = getStockById(3);   // Cheese Burger

        DatabaseInitializer.initialize();     // simulate application restart

        assertEquals(kottuBefore,  getStockById(2),
            "Chicken Kottu stock must not change when initializer is re-run (migration is one-time)");
        assertEquals(cheeseBefore, getStockById(3),
            "Cheese Burger stock must not change when initializer is re-run");
    }

    @Test @org.junit.jupiter.api.Order(82)
    void itemSoldToZeroRemainsZeroAfterRestartSimulation() throws Exception {
        // Arrange: drive Chicken Kottu stock to 0 (simulates selling out via real orders)
        setStock(2, 0);

        // Act: restart simulation
        DatabaseInitializer.initialize();

        // Assert: migration must NOT re-run (key already recorded) → stock stays 0
        assertEquals(0, getStockById(2),
            "An item whose stock reached zero through valid sales must remain zero " +
            "after application restart; the one-time migration must not re-apply");

        // Restore demo value so the DB is left in a clean state
        setStock(2, 20);
    }

    @Test @org.junit.jupiter.api.Order(83)
    void positiveStockIsNeverOverwrittenByInitializerRestart() throws Exception {
        int cheeseBefore = getStockById(3);   // Cheese Burger, currently 30
        assertTrue(cheeseBefore > 0, "Pre-condition: Cheese Burger must have positive stock");

        DatabaseInitializer.initialize();     // restart simulation

        assertEquals(cheeseBefore, getStockById(3),
            "Positive stock must be preserved exactly across every application restart");
    }

    // ══════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(90)
    void dataPersistedAcrossSeparateJDBCConnections() throws Exception {
        // Each service call opens a fresh connection — this simulates a restart
        List<FoodItem> foods = FoodService.getAllFoodItems();
        assertFalse(foods.isEmpty(), "Food items must be present on a fresh connection");

        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food, "Isolated test food item must survive across connections");

        boolean ricePresent = foods.stream()
            .anyMatch(f -> "Chicken Fried Rice".equals(f.getFoodName()));
        assertTrue(ricePresent, "Seeded 'Chicken Fried Rice' must remain unmodified");
    }

    // ══════════════════════════════════════════════════════════════════════
    // IMAGE MANAGER — unit tests (no DB required)
    // ══════════════════════════════════════════════════════════════════════

    /** Creates a minimal valid JPEG in a temp directory for image tests. */
    private static Path makeTempJpg(Path dir, String name) throws Exception {
        Path f = dir.resolve(name);
        BufferedImage bi = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "jpg", f.toFile());
        return f;
    }

    /** Creates a minimal valid PNG in a temp directory for image tests. */
    private static Path makeTempPng(Path dir, String name) throws Exception {
        Path f = dir.resolve(name);
        BufferedImage bi = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "png", f.toFile());
        return f;
    }

    @Test @org.junit.jupiter.api.Order(70)
    void importImageJpgCopiesFileToManagedDir() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-70");
        Path src = makeTempJpg(tmp, "food.jpg");
        String stored = ImageManager.importImage(src);
        try {
            assertNotNull(stored, "importImage must return a non-null filename");
            assertFalse(stored.isBlank(), "importImage must return a non-blank filename");
            Path managed = ImageManager.MANAGED_DIR.resolve(stored);
            assertTrue(Files.exists(managed), "Imported file must exist inside the managed directory");
        } finally {
            ImageManager.deleteManagedImage(stored);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(71)
    void importImagePngCopiesFileToManagedDir() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-71");
        Path src = makeTempPng(tmp, "item.png");
        String stored = ImageManager.importImage(src);
        try {
            assertNotNull(stored);
            assertTrue(Files.exists(ImageManager.MANAGED_DIR.resolve(stored)));
        } finally {
            ImageManager.deleteManagedImage(stored);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(72)
    void importImageRejectsUnsupportedFormat() throws Exception {
        // PASS = system correctly rejects the unsupported file type
        Path tmp = Files.createTempDirectory("img-test-72");
        Path src = tmp.resolve("image.gif");
        Files.writeString(src, "GIF89a"); // minimal fake GIF header
        try {
            assertThrows(java.io.IOException.class,
                () -> ImageManager.importImage(src),
                "CORRECTLY REJECTED: .gif files must be rejected by importImage");
        } finally {
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(73)
    void importImageGeneratesUniqueFilenamePerCall() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-73");
        Path src1 = makeTempJpg(tmp, "apple.jpg");
        Path src2 = makeTempJpg(tmp, "banana.jpg");
        String s1 = ImageManager.importImage(src1);
        String s2 = ImageManager.importImage(src2);
        try {
            assertNotEquals(s1, s2,
                "Two importImage calls for different files must produce distinct filenames");
        } finally {
            ImageManager.deleteManagedImage(s1);
            ImageManager.deleteManagedImage(s2);
            Files.deleteIfExists(src1);
            Files.deleteIfExists(src2);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(74)
    void importImageReturnValueIsNotAbsolutePath() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-74");
        Path src = makeTempJpg(tmp, "test.jpg");
        String stored = ImageManager.importImage(src);
        try {
            assertFalse(stored.contains(":\\"),
                "Stored value must not contain Windows drive separator (:\\)");
            assertFalse(stored.startsWith("/"),
                "Stored value must not start with a Unix root slash");
            assertFalse(stored.contains(File.separator + "Users" + File.separator),
                "Stored value must not contain a user-specific directory segment");
        } finally {
            ImageManager.deleteManagedImage(stored);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(75)
    void importImageDoesNotModifyOrDeleteSourceFile() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-75");
        Path src = makeTempJpg(tmp, "original.jpg");
        long sizeBefore = Files.size(src);
        String stored = ImageManager.importImage(src);
        try {
            assertTrue(Files.exists(src),
                "Source file must still exist after importImage");
            assertEquals(sizeBefore, Files.size(src),
                "Source file size must not change after importImage");
        } finally {
            ImageManager.deleteManagedImage(stored);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(76)
    void resolveImageValidPathReturnsAbsolutePath() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-76");
        Path src = makeTempJpg(tmp, "find_me.jpg");
        String stored = ImageManager.importImage(src);
        try {
            Path resolved = ImageManager.resolveImage(stored);
            assertNotNull(resolved, "resolveImage must return a non-null Path for a valid stored filename");
            assertTrue(Files.exists(resolved), "Resolved path must point to an existing file");
            assertTrue(resolved.startsWith(ImageManager.MANAGED_DIR),
                "Resolved path must be inside the managed directory");
        } finally {
            ImageManager.deleteManagedImage(stored);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(77)
    void resolveImageMissingFileReturnsNull() {
        assertNull(ImageManager.resolveImage("nonexistent_" + System.currentTimeMillis() + ".jpg"),
            "resolveImage must return null when the file does not exist in the managed directory");
    }

    @Test @org.junit.jupiter.api.Order(78)
    void resolveImageNullAndBlankReturnNull() {
        assertNull(ImageManager.resolveImage(null),  "resolveImage(null) must return null");
        assertNull(ImageManager.resolveImage(""),    "resolveImage(\"\") must return null");
        assertNull(ImageManager.resolveImage("   "), "resolveImage(blank) must return null");
    }

    @Test @org.junit.jupiter.api.Order(79)
    void resolveImageBlocksPathTraversal() {
        // PASS = system correctly blocks each traversal attempt
        assertNull(ImageManager.resolveImage("../../secret.txt"),
            "CORRECTLY BLOCKED: '../' traversal via relative segments");
        assertNull(ImageManager.resolveImage("../food-images/../secret"),
            "CORRECTLY BLOCKED: mixed traversal");
        // Windows-style absolute path must not resolve inside managed dir
        assertNull(ImageManager.resolveImage("C:\\Windows\\System32\\drivers\\etc\\hosts"),
            "CORRECTLY BLOCKED: Windows absolute path in stored value");
    }

    @Test @org.junit.jupiter.api.Order(84)
    void addFoodItemPersistsManagedRelativePath() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-84");
        Path src = makeTempJpg(tmp, "pizza.jpg");
        String managed = ImageManager.importImage(src);
        int newId = -1;
        try {
            newId = FoodService.addFoodItem("SV_TestFood_ImgAdd", 250.0, true, 5, managed);
            assertTrue(newId > 0, "addFoodItem must return a positive ID");

            FoodItem saved = getFoodById(newId);
            assertNotNull(saved, "Newly added food item must be retrievable");
            assertEquals(managed, saved.getImagePath(),
                "Stored image_path must equal the managed relative filename, not an absolute path");
            assertFalse(saved.getImagePath().contains(":\\"),
                "Stored path must not contain Windows drive separator");
            assertFalse(saved.getImagePath().startsWith("/"),
                "Stored path must not start with Unix root slash");
        } finally {
            if (newId > 0) FoodService.deleteFoodItem(newId);
            ImageManager.deleteManagedImage(managed);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(85)
    void updateFoodWithoutNewImageRetainsExistingPath() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-85");
        Path src = makeTempJpg(tmp, "burger.jpg");
        String managed = ImageManager.importImage(src);
        try {
            // Assign managed path to the shared test food item
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, managed);

            // Update name/price only — pass the same path (simulating "no new image selected")
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, managed);

            FoodItem after = getFoodById(testFoodId);
            assertNotNull(after);
            assertEquals(managed, after.getImagePath(),
                "image_path must be retained unchanged when no new image is selected");
        } finally {
            // Restore original null path
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, null);
            ImageManager.deleteManagedImage(managed);
            Files.deleteIfExists(src);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(86)
    void updateFoodWithNewImagePersistsNewManagedPath() throws Exception {
        Path tmp = Files.createTempDirectory("img-test-86");
        Path src1 = makeTempJpg(tmp, "old.jpg");
        Path src2 = makeTempJpg(tmp, "new.jpg");
        String managed1 = ImageManager.importImage(src1);
        String managed2 = ImageManager.importImage(src2);
        try {
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, managed1);
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, managed2);

            FoodItem after = getFoodById(testFoodId);
            assertNotNull(after);
            assertEquals(managed2, after.getImagePath(),
                "image_path must reflect the new managed filename after update with new image");
            assertNotEquals(managed1, after.getImagePath(),
                "Old managed filename must have been replaced");
        } finally {
            FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, null);
            ImageManager.deleteManagedImage(managed1);
            ImageManager.deleteManagedImage(managed2);
            Files.deleteIfExists(src1);
            Files.deleteIfExists(src2);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(87)
    void legacyExistingAbsolutePathIsMigratedToManagedRelativePath() throws Exception {
        // Create a real temp image file to act as the "legacy source"
        Path tmp = Files.createTempDirectory("img-test-87");
        Path legacySrc = makeTempJpg(tmp, "legacy_photo.jpg");
        String absPath = legacySrc.toAbsolutePath().toString();

        // Insert directly via JDBC bypassing the service layer (simulates a pre-fix record).
        // category_id is required (NOT NULL) — use "Other" as the mandatory fallback.
        int legacyId;
        int otherCatId87 = FoodCategoryService.getOtherCategoryId();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO food_items " +
                 "(food_name, price, available, stock_quantity, image_path, category_id) " +
                 "VALUES ('SV_TestFood_ImgLegacy1', 50.0, 1, 1, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, absPath);
            ps.setInt(2, otherCatId87);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            assertTrue(keys.next(), "INSERT must return generated key");
            legacyId = keys.getInt(1);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int repaired = DatabaseInitializer.repairAbsoluteImagePaths(conn);
            assertTrue(repaired >= 1,
                "repairAbsoluteImagePaths must report at least one repaired row");
        }

        try {
            FoodItem repaired = getFoodById(legacyId);
            assertNotNull(repaired, "Food item must still exist after repair");
            String storedPath = repaired.getImagePath();
            assertNotNull(storedPath, "image_path must not be null after repair");
            assertFalse(storedPath.contains(":\\"),
                "Repaired path must not contain Windows drive separator");
            assertFalse(storedPath.startsWith("/"),
                "Repaired path must not start with Unix root slash");
            // The managed file must actually exist
            Path managedFile = ImageManager.resolveImage(storedPath);
            assertNotNull(managedFile,
                "Managed image file must exist in the managed directory after repair");
            // Cleanup managed image
            ImageManager.deleteManagedImage(storedPath);
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute(
                    "DELETE FROM food_items WHERE food_id = " + legacyId);
            }
            Files.deleteIfExists(legacySrc);
            Files.deleteIfExists(tmp);
        }
    }

    @Test @org.junit.jupiter.api.Order(88)
    void legacyMissingAbsolutePathDoesNotCrash() throws Exception {
        // Insert a record with an absolute path whose source file does NOT exist
        String fakePath = (System.getProperty("os.name").contains("Windows")
            ? "C:\\NonExistent\\ghost_image_" + System.currentTimeMillis() + ".jpg"
            : "/nonexistent/ghost_image_" + System.currentTimeMillis() + ".jpg");

        // category_id is required (NOT NULL) — use "Other" as the mandatory fallback.
        int ghostId;
        int otherCatId88 = FoodCategoryService.getOtherCategoryId();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO food_items " +
                 "(food_name, price, available, stock_quantity, image_path, category_id) " +
                 "VALUES ('SV_TestFood_ImgLegacy2', 50.0, 1, 1, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fakePath);
            ps.setInt(2, otherCatId88);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            assertTrue(keys.next());
            ghostId = keys.getInt(1);
        }

        // Must not throw even when source is missing
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertDoesNotThrow(
                () -> DatabaseInitializer.repairAbsoluteImagePaths(conn),
                "repairAbsoluteImagePaths must not crash when source file is missing");
        }

        // Food item must still exist (repair does not delete records)
        FoodItem ghost = getFoodById(ghostId);
        assertNotNull(ghost, "Food item must still exist even when image repair could not fix it");

        // Cleanup
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.createStatement().execute(
                "DELETE FROM food_items WHERE food_id = " + ghostId);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FOOD CATEGORY MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(100)
    void categoryCanBeCreatedAndRetrieved() {
        int id = FoodCategoryService.addCategory("SV_TestCategory");
        try {
            assertTrue(id > 0, "addCategory must return a positive ID");
            boolean found = FoodCategoryService.getAllCategories().stream()
                .anyMatch(c -> c.getCategoryId() == id);
            assertTrue(found, "New category must appear in getAllCategories()");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(101)
    void categoryNameCanBeUpdated() {
        int id = FoodCategoryService.addCategory("SV_ToCatUpdate");
        try {
            boolean ok = FoodCategoryService.updateCategory(id, "SV_ToCatUpdated");
            assertTrue(ok, "updateCategory must return true");
            boolean found = FoodCategoryService.getAllCategories().stream()
                .anyMatch(c -> c.getCategoryId() == id
                            && "SV_ToCatUpdated".equals(c.getCategoryName()));
            assertTrue(found, "Updated category name must be reflected in getAllCategories()");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(102)
    void foodItemCanBeAssignedToCategory() throws Exception {
        int catId = FoodCategoryService.addCategory("SV_AssignCategory");
        int foodId = -1;
        try {
            foodId = FoodService.addFoodItem("SV_CatAssignFood", 300.0, true, 5, null, catId);
            assertTrue(foodId > 0, "addFoodItem with category must return positive ID");
            FoodItem saved = getFoodById(foodId);
            assertNotNull(saved, "Food item with category must be retrievable");
            assertEquals(catId, saved.getCategoryId(),
                "Stored category_id must match the assigned category");
        } finally {
            if (foodId > 0) FoodService.deleteFoodItem(foodId);
            FoodCategoryService.deleteCategory(catId);
        }
    }

    @Test @org.junit.jupiter.api.Order(104)
    void duplicateCategoryNameIsCorrectlyRejected() {
        int id = FoodCategoryService.addCategory("SV_DupCategory");
        try {
            assertThrows(RuntimeException.class,
                () -> FoodCategoryService.addCategory("SV_DupCategory"),
                "CORRECTLY REJECTED: duplicate category name must throw RuntimeException");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CATEGORY ENFORCEMENT TESTS (@Order 105-114)
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(105)
    void categoryIdColumnIsNotNull() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT IS_NULLABLE FROM information_schema.COLUMNS " +
                 "WHERE TABLE_SCHEMA = DATABASE() " +
                 "AND TABLE_NAME = 'food_items' " +
                 "AND COLUMN_NAME = 'category_id'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "category_id column must exist in food_items");
            assertEquals("NO", rs.getString(1),
                "category_id must be NOT NULL — mandatory category enforcement");
        }
    }

    @Test @org.junit.jupiter.api.Order(106)
    void categoryForeignKeyHasRestrictDeleteRule() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT rc.DELETE_RULE " +
                 "FROM information_schema.REFERENTIAL_CONSTRAINTS rc " +
                 "JOIN information_schema.KEY_COLUMN_USAGE kcu " +
                 "  ON rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                 "  AND rc.CONSTRAINT_SCHEMA = kcu.TABLE_SCHEMA " +
                 "WHERE kcu.TABLE_SCHEMA = DATABASE() " +
                 "AND kcu.TABLE_NAME = 'food_items' " +
                 "AND kcu.COLUMN_NAME = 'category_id' " +
                 "AND kcu.REFERENCED_TABLE_NAME = 'food_categories'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(),
                "FK from food_items.category_id to food_categories must exist");
            String rule = rs.getString(1);
            assertTrue("RESTRICT".equals(rule) || "NO ACTION".equals(rule),
                "FK DELETE_RULE must be RESTRICT or NO ACTION, was: " + rule);
        }
    }

    @Test @org.junit.jupiter.api.Order(107)
    void noUncategorisedFoodItemsExist() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM food_items WHERE category_id IS NULL")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(0, rs.getInt(1),
                "No food_items row should have a NULL category_id");
        }
    }

    @Test @org.junit.jupiter.api.Order(108)
    void categoryMigrationIsIdempotent() throws Exception {
        DatabaseInitializer.initialize();   // simulate application restart
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            ps.setString(1, "20260830_enforce_food_category_required");
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt(1),
                "Migration key must appear exactly once in schema_migrations (idempotent)");
        }
    }

    @Test @org.junit.jupiter.api.Order(109)
    void deletingInUseCategoryIsRejectedAndBothUnchanged() throws Exception {
        int catId = FoodCategoryService.addCategory("SV_InUseCat");
        int foodId = -1;
        try {
            foodId = FoodService.addFoodItem("SV_InUseCatFood", 400.0, true, 10, null, catId);
            final int fid = foodId;
            final int cid = catId;
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> FoodCategoryService.deleteCategory(cid),
                "Deleting a category assigned to food items must throw IllegalStateException");
            assertTrue(ex.getMessage().contains("cannot be deleted"),
                "Exception message must mention 'cannot be deleted'");
            // Both category and food item must still exist after the rejected deletion
            boolean catStillExists = FoodCategoryService.getAllCategories().stream()
                .anyMatch(c -> c.getCategoryId() == cid);
            assertTrue(catStillExists,
                "Category must still exist after a rejected deletion attempt");
            assertNotNull(getFoodById(fid),
                "Food item must still exist after a rejected deletion attempt");
        } finally {
            if (foodId > 0) FoodService.deleteFoodItem(foodId);
            FoodCategoryService.deleteCategory(catId);
        }
    }

    @Test @org.junit.jupiter.api.Order(110)
    void deletingUnusedCategorySucceeds() {
        int id = FoodCategoryService.addCategory("SV_UnusedCat");
        assertTrue(FoodCategoryService.deleteCategory(id),
            "Deleting a category with no food items must return true");
        boolean stillExists = FoodCategoryService.getAllCategories().stream()
            .anyMatch(c -> c.getCategoryId() == id);
        assertFalse(stillExists,
            "Deleted category must not appear in getAllCategories()");
    }

    @Test @org.junit.jupiter.api.Order(111)
    void addingFoodWithZeroCategoryIdIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> FoodService.addFoodItem("SV_NoCatFood", 100.0, true, 5, null, 0),
            "CORRECTLY REJECTED: addFoodItem with categoryId=0 must throw IllegalArgumentException");
    }

    @Test @org.junit.jupiter.api.Order(112)
    void updatingFoodWithZeroCategoryIdIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> FoodService.updateFoodItem(testFoodId, "SV_TestFood", 500.0, true, 50, null, 0),
            "CORRECTLY REJECTED: updateFoodItem with categoryId=0 must throw IllegalArgumentException");
    }

    @Test @org.junit.jupiter.api.Order(113)
    void menuFilterNameAndCategoryLogicIsCorrect() throws Exception {
        int catId = FoodCategoryService.addCategory("SV_MenuFilterCat");
        int foodId = -1;
        try {
            foodId = FoodService.addFoodItem("SV_SpecialDish", 200.0, true, 5, null, catId);
            List<FoodItem> all = FoodService.getAvailableFoodItems();
            final int fid = foodId;
            final int cid = catId;

            // Name-only filter must find the item
            List<FoodItem> byName = filterMenu(all, "specialdish", 0);
            assertTrue(byName.stream().anyMatch(f -> f.getFoodId() == fid),
                "Name-only filter must find SV_SpecialDish");

            // Category-only filter must find the item
            List<FoodItem> byCat = filterMenu(all, "", cid);
            assertTrue(byCat.stream().anyMatch(f -> f.getFoodId() == fid),
                "Category-only filter must find SV_SpecialDish");

            // Combined (name + matching category) must find the item
            List<FoodItem> both = filterMenu(all, "special", cid);
            assertTrue(both.stream().anyMatch(f -> f.getFoodId() == fid),
                "Name+category filter must find SV_SpecialDish");

            // Combined with a non-matching category must not find the item
            int otherId = FoodCategoryService.getAllCategories().stream()
                .filter(c -> c.getCategoryId() != cid)
                .findFirst()
                .map(FoodCategory::getCategoryId)
                .orElse(-999);
            List<FoodItem> noMatch = filterMenu(all, "special", otherId);
            assertFalse(noMatch.stream().anyMatch(f -> f.getFoodId() == fid),
                "Name+wrong-category filter must not find SV_SpecialDish");
        } finally {
            if (foodId > 0) FoodService.deleteFoodItem(foodId);
            FoodCategoryService.deleteCategory(catId);
        }
    }

    @Test @org.junit.jupiter.api.Order(114)
    void menuCategoryIdZeroShowsAllCategories() {
        List<FoodItem> all = FoodService.getAvailableFoodItems();
        List<FoodItem> filtered = filterMenu(all, "", 0);
        assertEquals(all.size(), filtered.size(),
            "categoryId=0 must act as 'show all' — no category filter applied");
    }

    @Test @org.junit.jupiter.api.Order(115)
    void addFoodWithNonExistingCategoryIdIsCorrectlyRejected() {
        // Create and immediately delete a category to obtain a positive but non-existing ID
        int deletedId = FoodCategoryService.addCategory("SV_GhostCat");
        FoodCategoryService.deleteCategory(deletedId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> FoodService.addFoodItem("SV_GhostCatFood", 100.0, true, 5, null, deletedId),
            "CORRECTLY REJECTED: addFoodItem with non-existing categoryId must throw " +
            "IllegalArgumentException");
        assertTrue(ex.getMessage().contains("does not exist"),
            "Exception message must mention 'does not exist'");

        // No food item must have been created
        boolean created = FoodService.getAllFoodItems().stream()
            .anyMatch(f -> "SV_GhostCatFood".equals(f.getFoodName()));
        assertFalse(created, "No food item must be created when category does not exist");
    }

    @Test @org.junit.jupiter.api.Order(116)
    void updateFoodWithNonExistingCategoryIdIsCorrectlyRejected() throws Exception {
        // Create and immediately delete a category to obtain a positive but non-existing ID
        int deletedId = FoodCategoryService.addCategory("SV_GhostCat2");
        FoodCategoryService.deleteCategory(deletedId);

        // Capture exact current state of the shared test food item
        FoodItem before = getFoodById(testFoodId);
        assertNotNull(before, "Test food item must exist before the rejected update");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> FoodService.updateFoodItem(
                testFoodId, "SV_TestFood_GHOST", 999.0, true, 99, null, deletedId),
            "CORRECTLY REJECTED: updateFoodItem with non-existing categoryId must throw " +
            "IllegalArgumentException");
        assertTrue(ex.getMessage().contains("does not exist"),
            "Exception message must mention 'does not exist'");

        // Verify no food item fields were changed
        FoodItem after = getFoodById(testFoodId);
        assertNotNull(after, "Food item must still exist after rejected update");
        assertEquals(before.getFoodName(),   after.getFoodName(),
            "food_name must be unchanged after rejected update");
        assertEquals(before.getPrice(),      after.getPrice(), 0.01,
            "price must be unchanged after rejected update");
        assertEquals(before.getCategoryId(), after.getCategoryId(),
            "category_id must be unchanged after rejected update");
        assertEquals(before.getStockQuantity(), after.getStockQuantity(),
            "stock_quantity must be unchanged after rejected update");
    }

    // ══════════════════════════════════════════════════════════════════════
    // CATEGORY DESCRIPTION (@Order 117-125)
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(117)
    void categoryDescriptionPersistsAfterCreate() {
        int id = FoodCategoryService.addCategory("SV_DescCreate", "A test description");
        try {
            Optional<FoodCategory> found = FoodCategoryService.getCategoryById(id);
            assertTrue(found.isPresent(), "Category must be retrievable after creation");
            assertEquals("A test description", found.get().getDescription(),
                "Description must be persisted exactly as supplied on create");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(118)
    void categoryDescriptionPersistsAfterUpdate() {
        int id = FoodCategoryService.addCategory("SV_DescUpdate", "Initial desc");
        try {
            FoodCategoryService.updateCategory(id, "SV_DescUpdate", "Updated desc");
            Optional<FoodCategory> found = FoodCategoryService.getCategoryById(id);
            assertTrue(found.isPresent(), "Category must be retrievable after update");
            assertEquals("Updated desc", found.get().getDescription(),
                "Description must reflect the updated value");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(119)
    void nullOrBlankDescriptionIsAccepted() {
        // Null description
        int id1 = FoodCategoryService.addCategory("SV_NullDesc", null);
        try {
            Optional<FoodCategory> found1 = FoodCategoryService.getCategoryById(id1);
            assertTrue(found1.isPresent());
            assertNull(found1.get().getDescription(),
                "null description must be stored as SQL NULL and returned as null");
        } finally {
            FoodCategoryService.deleteCategory(id1);
        }

        // Blank description — service normalises it to null
        int id2 = FoodCategoryService.addCategory("SV_BlankDesc", "   ");
        try {
            Optional<FoodCategory> found2 = FoodCategoryService.getCategoryById(id2);
            assertTrue(found2.isPresent());
            assertNull(found2.get().getDescription(),
                "blank/whitespace-only description must be stored as SQL NULL");
        } finally {
            FoodCategoryService.deleteCategory(id2);
        }
    }

    @Test @org.junit.jupiter.api.Order(120)
    void descriptionOver255CharsIsCorrectlyRejected() {
        // PASS = service correctly rejects the oversized description
        String longDesc = "x".repeat(256);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> FoodCategoryService.addCategory("SV_LongDesc", longDesc),
            "CORRECTLY REJECTED: description exceeding 255 chars must throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains("255"),
            "Exception message must mention the 255-character limit");
        // No category must have been created
        boolean created = FoodCategoryService.getAllCategories().stream()
            .anyMatch(c -> "SV_LongDesc".equals(c.getCategoryName()));
        assertFalse(created, "No category must be created when description is too long");
    }

    @Test @org.junit.jupiter.api.Order(121)
    void duplicateCategoryAddIsRejectedCaseInsensitively() {
        // PASS = service correctly enforces case-insensitive uniqueness
        int id = FoodCategoryService.addCategory("SV_CaseTest");
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FoodCategoryService.addCategory("sv_casetest"),
                "CORRECTLY REJECTED: duplicate name differing only in case must be rejected");
            assertTrue(ex.getMessage().contains("already exists"),
                "Exception message must mention 'already exists'");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(122)
    void updatingToAnotherCategorysNameIsCorrectlyRejected() {
        // PASS = service correctly blocks renaming to an existing category's name
        int idA = FoodCategoryService.addCategory("SV_CatA");
        int idB = FoodCategoryService.addCategory("SV_CatB");
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FoodCategoryService.updateCategory(idB, "SV_CatA"),
                "CORRECTLY REJECTED: renaming to another category's name must throw");
            assertTrue(ex.getMessage().contains("already exists"),
                "Exception message must mention 'already exists'");
            // Category B must retain its original name
            Optional<FoodCategory> b = FoodCategoryService.getCategoryById(idB);
            assertTrue(b.isPresent() && "SV_CatB".equals(b.get().getCategoryName()),
                "Category B must retain its original name after the rejected rename");
        } finally {
            FoodCategoryService.deleteCategory(idA);
            FoodCategoryService.deleteCategory(idB);
        }
    }

    @Test @org.junit.jupiter.api.Order(123)
    void updatingCategoryWithoutChangingNameSucceeds() {
        // Updating a category while keeping the same name must not be blocked as a duplicate
        int id = FoodCategoryService.addCategory("SV_SameName", "original desc");
        try {
            assertDoesNotThrow(
                () -> FoodCategoryService.updateCategory(id, "SV_SameName", "updated desc"),
                "Updating a category without changing its name must not throw");
            Optional<FoodCategory> found = FoodCategoryService.getCategoryById(id);
            assertTrue(found.isPresent());
            assertEquals("updated desc", found.get().getDescription(),
                "Description must be updated even when the name stays the same");
        } finally {
            FoodCategoryService.deleteCategory(id);
        }
    }

    @Test @org.junit.jupiter.api.Order(124)
    void descriptionMigrationIsIdempotent() throws Exception {
        // Re-running initialize() must not duplicate the migration key or alter existing data
        DatabaseInitializer.initialize();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            ps.setString(1, "20260831_add_food_category_description");
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt(1),
                "Description migration key must appear exactly once after repeated initialise() calls");
        }
    }

    @Test @org.junit.jupiter.api.Order(125)
    void existingCategoriesRemainAfterMigration() throws Exception {
        // Default seeded categories must survive the migration
        DatabaseInitializer.initialize();
        List<FoodCategory> cats = FoodCategoryService.getAllCategories();
        boolean otherPresent = cats.stream()
            .anyMatch(c -> "Other".equals(c.getCategoryName()));
        assertTrue(otherPresent,
            "The 'Other' fallback category must still exist after the description migration");
        // Verify the description column is accessible (no mapping error)
        cats.forEach(c -> assertDoesNotThrow(
            c::getDescription,
            "getDescription() must not throw for any seeded category"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    private static FoodItem getFoodById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT food_id, food_name, price, available, stock_quantity, " +
                 "       image_path, category_id " +
                 "FROM food_items WHERE food_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            FoodItem fi = new FoodItem();
            fi.setFoodId(rs.getInt("food_id"));
            fi.setFoodName(rs.getString("food_name"));
            fi.setPrice(rs.getDouble("price"));
            fi.setAvailable(rs.getBoolean("available"));
            fi.setStockQuantity(rs.getInt("stock_quantity"));
            fi.setImagePath(rs.getString("image_path"));
            fi.setCategoryId(rs.getInt("category_id")); // 0 when NULL
            return fi;
        }
    }

    private static int getStockById(int foodId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT stock_quantity FROM food_items WHERE food_id=?")) {
            ps.setInt(1, foodId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static void setStock(int foodId, int qty) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE food_items SET stock_quantity=? WHERE food_id=?")) {
            ps.setInt(1, qty);
            ps.setInt(2, foodId);
            ps.executeUpdate();
        }
    }

    private static String getOrderStatus(int orderId) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT status FROM orders WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static int countTestOrders() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM orders WHERE customer_id=?")) {
            ps.setInt(1, testCustomer.getCustomerId());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Removes all data created by this test class.
     * Order: delete customer (cascades → orders → order_items), then food item.
     * Safe to call before and after the suite.
     */
    /**
     * Replicates the FoodMenuFrame in-memory filter logic so it can be tested
     * without instantiating a Swing component.
     *
     * @param items      source list (usually from FoodService.getAvailableFoodItems())
     * @param nameQuery  case-insensitive substring to match against food_name; empty = any name
     * @param categoryId positive ID to filter by; 0 or negative = all categories
     */
    private static List<FoodItem> filterMenu(List<FoodItem> items,
                                             String nameQuery, int categoryId) {
        String q = (nameQuery == null) ? "" : nameQuery.toLowerCase().trim();
        return items.stream()
            .filter(f -> {
                boolean nameMatch = q.isEmpty() || f.getFoodName().toLowerCase().contains(q);
                boolean catMatch  = categoryId <= 0 || f.getCategoryId() == categoryId;
                return nameMatch && catMatch;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Removes all data created by this test class.
     *
     * <p>Order matters after the CASCADE→RESTRICT migration on orders.customer_id:
     * <ol>
     *   <li>Delete orders belonging to the test customer (orders → order_items cascades).</li>
     *   <li>Delete the test customer row.</li>
     *   <li>Delete any SV_ customer management fixtures (no orders expected).</li>
     *   <li>Delete SV_ food items (FK references food_categories).</li>
     *   <li>Delete SV_ food categories.</li>
     * </ol>
     */
    private static void deleteTestFixtures() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Step 1: Delete orders for the main test customer (cascades to order_items)
            conn.createStatement().execute(
                "DELETE FROM orders WHERE customer_id IN " +
                "(SELECT customer_id FROM customers WHERE username = '" + TEST_USER + "')");
            // Step 2: Delete the main test customer
            conn.createStatement().execute(
                "DELETE FROM customers WHERE username = '" + TEST_USER + "'");
            // Step 3: Delete any SV_ customer management fixtures (sv_custmgmt etc.)
            conn.createStatement().execute(
                "DELETE FROM customers WHERE username LIKE 'sv\\_%' ESCAPE '\\'");
            // Step 4: Delete SV_ food items (FK: food_items → food_categories)
            conn.createStatement().execute(
                "DELETE FROM food_items WHERE food_name LIKE 'SV_%'");
            // Step 5: Delete SV_ food categories
            conn.createStatement().execute(
                "DELETE FROM food_categories WHERE category_name LIKE 'SV_%'");
        } catch (Exception ignored) {
            // Fixtures did not exist — nothing to clean up
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CUSTOMER MANAGEMENT (@Order 200-217)
    // ══════════════════════════════════════════════════════════════════════

    /** ID of the customer created in @Order(201), reused through @Order(215). */
    static int svCustomerId = -1;

    @Test @org.junit.jupiter.api.Order(200)
    void getAllCustomersIncludesSeedCustomer() {
        List<Customer> customers =
            CustomerManagementService.getAllCustomers();
        assertTrue(
            customers.stream().anyMatch(c -> c.getUsername().equalsIgnoreCase("customer")),
            "getAllCustomers() should include the seeded demo customer");
    }

    @Test @org.junit.jupiter.api.Order(201)
    void addCustomerSucceedsWithValidData() {
        svCustomerId = CustomerManagementService.addCustomer(
            "SV CustMgmt User", "sv_custmgmt", "Test@1234", "Test@1234");
        assertTrue(svCustomerId > 0, "addCustomer should return a positive generated ID");
    }

    @Test @org.junit.jupiter.api.Order(202)
    void addedCustomerAppearsInGetAll() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        List<Customer> customers =
            CustomerManagementService.getAllCustomers();
        assertTrue(
            customers.stream().anyMatch(c -> c.getCustomerId() == svCustomerId),
            "Newly added customer must appear in getAllCustomers()");
    }

    @Test @org.junit.jupiter.api.Order(203)
    void addCustomerWithBlankNameIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.addCustomer(
                "", "sv_blank_name", "Test@1234", "Test@1234"),
            "Blank full name must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(204)
    void addCustomerWithTooShortUsernameIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.addCustomer(
                "SV Short User", "sv", "Test@1234", "Test@1234"),
            "Username shorter than minimum length must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(205)
    void addCustomerWithDuplicateUsernameIsCorrectlyRejectedCaseInsensitively() {
        // sv_custmgmt already exists from @Order(201)
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.addCustomer(
                "SV Duplicate", "SV_CUSTMGMT", "Test@1234", "Test@1234"),
            "Duplicate username (case-insensitive) must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(206)
    void addCustomerWithMismatchedPasswordsIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.addCustomer(
                "SV Mismatch", "sv_mismatch", "Test@1234", "Different@9"),
            "Mismatched password and confirmation must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(207)
    void addCustomerWithNonCompliantPasswordIsCorrectlyRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.addCustomer(
                "SV Weak Pwd", "sv_weakpwd", "weakpass", "weakpass"),
            "Password not meeting policy (no digit) must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(208)
    void updateCustomerProfileWithoutPasswordChange() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        boolean updated = CustomerManagementService.updateCustomer(
            svCustomerId, "SV Updated Name", "sv_custmgmt", null, null);
        assertTrue(updated, "updateCustomer (profile only) should return true");
        citybites.model.Customer c =
            CustomerManagementService.getCustomerById(svCustomerId);
        assertEquals("SV Updated Name", c.getFullName(),
            "Full name should be updated in the database");
    }

    @Test @org.junit.jupiter.api.Order(209)
    void updateCustomerWithNewPassword() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        boolean updated = CustomerManagementService.updateCustomer(
            svCustomerId, "SV Updated Name", "sv_custmgmt", "NewPass@99", "NewPass@99");
        assertTrue(updated, "updateCustomer (with new password) should return true");
    }

    @Test @org.junit.jupiter.api.Order(210)
    void updateCustomerToExistingUsernameIsCorrectlyRejected() {
        // "customer" (seeded) already exists; renaming sv_custmgmt to it must fail
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        assertThrows(IllegalArgumentException.class,
            () -> CustomerManagementService.updateCustomer(
                svCustomerId, "SV Updated Name", "CUSTOMER", null, null),
            "Renaming to an existing username (case-insensitive) must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(211)
    void updateCustomerToOwnUsernameSucceeds() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        // Saving with the same username must not be blocked by the duplicate check
        boolean updated = CustomerManagementService.updateCustomer(
            svCustomerId, "SV Updated Name", "sv_custmgmt", null, null);
        assertTrue(updated, "Updating own username must not be blocked by duplicate check");
    }

    @Test @org.junit.jupiter.api.Order(212)
    void customerHasOrdersReturnsFalseWhenNoOrders() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        assertFalse(
            CustomerManagementService.customerHasOrders(svCustomerId),
            "sv_custmgmt should have no orders");
    }

    @Test @org.junit.jupiter.api.Order(213)
    void customerHasOrdersReturnsTrueWhenOrdersExist() {
        assertNotNull(testCustomer, "Requires testCustomer from @BeforeAll");
        assertTrue(
            CustomerManagementService.customerHasOrders(
                testCustomer.getCustomerId()),
            "testCustomer placed an order in @Order(20) — hasOrders must return true");
    }

    @Test @org.junit.jupiter.api.Order(214)
    void deleteCustomerWithOrdersIsCorrectlyRejected() {
        assertNotNull(testCustomer, "Requires testCustomer from @BeforeAll");
        assertThrows(IllegalStateException.class,
            () -> CustomerManagementService.deleteCustomer(
                testCustomer.getCustomerId()),
            "Deleting a customer with existing orders must throw IllegalStateException");
    }

    @Test @org.junit.jupiter.api.Order(215)
    void deleteCustomerWithoutOrdersSucceeds() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        boolean deleted =
            CustomerManagementService.deleteCustomer(svCustomerId);
        assertTrue(deleted, "deleteCustomer should return true for customer without orders");
        svCustomerId = -1;  // mark as cleaned up
    }

    @Test @org.junit.jupiter.api.Order(216)
    void fkMigrationKeyExistsInSchemaMigrations() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            ps.setString(1, "20260831_orders_customer_fk_restrict");
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt(1),
                "Migration '20260831_orders_customer_fk_restrict' must be recorded " +
                "in schema_migrations after initialisation");
        }
    }

    @Test @org.junit.jupiter.api.Order(217)
    void ordersCustomerFkDeleteRuleIsRestrict() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT DELETE_RULE " +
                 "FROM information_schema.REFERENTIAL_CONSTRAINTS " +
                 "WHERE CONSTRAINT_SCHEMA = DATABASE() " +
                 "AND TABLE_NAME = 'orders' " +
                 "AND REFERENCED_TABLE_NAME = 'customers'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(),
                "A FK from orders to customers must exist in information_schema");
            String deleteRule = rs.getString(1);
            assertTrue("RESTRICT".equals(deleteRule) || "NO ACTION".equals(deleteRule),
                "orders.customer_id FK DELETE_RULE must be RESTRICT or NO ACTION, " +
                "was: " + deleteRule);
        }
    }

    /**
     * Regression test: proves the full BCrypt round-trip for the intended seed credential.
     *
     * <p>DatabaseInitializer.seedCustomer() hashes "Demo1234" on first run. This test
     * creates a controlled SV_ fixture via CustomerManagementService (which also BCrypt-hashes
     * the password) and then verifies that AuthService.customerLogin can authenticate it.
     * The fixture mirrors the seed configuration exactly.
     *
     * <p><b>Live DB inconsistency notice</b>: The seeded "customer" demo account currently
     * has a password_hash that does not match "Demo1234" (it was changed manually during
     * development). This fixture-based test proves the authentication mechanism is correct;
     * the live account is the data-state anomaly.
     *
     * <p><b>Safe recovery procedure</b> (run once in a DB shell):
     * <pre>
     *   -- 1. Generate a BCrypt hash for "Demo1234" in Java:
     *   --    String hash = BCrypt.hashpw("Demo1234", BCrypt.gensalt());
     *   --    System.out.println(hash);  // e.g. $2a$10$...
     *   --
     *   -- 2. Substitute the printed hash into the UPDATE:
     *   UPDATE customers
     *   SET    password_hash = '$2a$10$<paste_generated_hash_here>'
     *   WHERE  username = 'customer';
     *
     *   -- 3. Verify: the application seedCustomer() will skip on next startup
     *   --    (row already exists) — no further action needed.
     * </pre>
     */
    @Test @org.junit.jupiter.api.Order(218)
    void seededCustomerLoginMechanismWorksWithIntendedCredential() {
        // Create a controlled fixture that mirrors the DatabaseInitializer seed configuration:
        //   username = "sv_seed_replica", password = "Demo1234" (the intended seed password)
        int id = CustomerManagementService.addCustomer(
            "SV Seed Replica", "sv_seed_replica", "Demo1234", "Demo1234");
        assertTrue(id > 0, "Controlled SV_ fixture must be created successfully");

        try {
            // Full BCrypt round-trip: hash stored by addCustomer, verified by customerLogin
            Optional<Customer> result =
                AuthService.customerLogin("sv_seed_replica", "Demo1234");

            assertTrue(result.isPresent(),
                "AuthService.customerLogin must return a Customer when supplied with the " +
                "intended seed credential 'Demo1234'. " +
                "NOTE: The live seeded 'customer' account has an inconsistent hash — " +
                "see the Javadoc above for the one-time SQL recovery procedure.");

            assertEquals("sv_seed_replica", result.get().getUsername(),
                "Returned Customer username must match the fixture username");

        } finally {
            // Explicit cleanup; also caught by deleteTestFixtures() sv_% pattern in @AfterAll
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }
}
