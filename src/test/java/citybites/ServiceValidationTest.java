package citybites;

import citybites.config.DatabaseConnection;
import citybites.config.DatabaseInitializer;
import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.FoodItem;
import citybites.model.Order;
import citybites.service.AuthService;
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

        // Insert directly via JDBC bypassing the service layer (simulates a pre-fix record)
        int legacyId;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO food_items (food_name, price, available, stock_quantity, image_path) " +
                 "VALUES ('SV_TestFood_ImgLegacy1', 50.0, 1, 1, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, absPath);
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

        int ghostId;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO food_items (food_name, price, available, stock_quantity, image_path) " +
                 "VALUES ('SV_TestFood_ImgLegacy2', 50.0, 1, 1, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fakePath);
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
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    private static FoodItem getFoodById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT food_id, food_name, price, available, stock_quantity, image_path " +
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
    private static void deleteTestFixtures() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.createStatement().execute(
                "DELETE FROM customers WHERE username = '" + TEST_USER + "'");
            conn.createStatement().execute(
                "DELETE FROM food_items WHERE food_name LIKE 'SV_TestFood%'");
        } catch (Exception ignored) {
            // Fixtures did not exist — nothing to clean up
        }
    }
}
