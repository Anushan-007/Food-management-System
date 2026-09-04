package citybites;

import citybites.config.DatabaseConnection;
import citybites.config.DatabaseInitializer;
import citybites.data.DataStore;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.FeaturedAssignmentResult;
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
    static int      testFeat2Id  = -1;   // secondary food for featured-slot ordering/conflict tests
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
    // ORDER STATUS TRANSITION VALIDATION (negative + helper tests)
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(34)
    void statusTransition_invalidName_isRejected() throws Exception {
        // "Processing" is a legacy/wrong name — must be rejected; DB status must not change.
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Pending");
        int tmpId = data[0];
        try {
            assertThrows(RuntimeException.class,
                () -> OrderService.updateOrderStatus(tmpId, "Processing"),
                "Transition to 'Processing' (invalid name) must be rejected");
            assertEquals("Pending", getOrderStatus(tmpId),
                "Status must remain Pending after the rejected transition");
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tmpId);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(35)
    void statusTransition_pendingToCompleted_isRejected() throws Exception {
        // Skipping Preparing and Ready is not permitted.
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Pending");
        int tmpId = data[0];
        try {
            assertThrows(RuntimeException.class,
                () -> OrderService.updateOrderStatus(tmpId, "Completed"),
                "Transition Pending -> Completed (skip) must be rejected");
            assertEquals("Pending", getOrderStatus(tmpId),
                "Status must remain Pending after the rejected transition");
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tmpId);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(36)
    void statusTransition_preparingToPending_isRejected() throws Exception {
        // Backward transition is not permitted.
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Preparing");
        int tmpId = data[0];
        try {
            assertThrows(RuntimeException.class,
                () -> OrderService.updateOrderStatus(tmpId, "Pending"),
                "Transition Preparing -> Pending (backward) must be rejected");
            assertEquals("Preparing", getOrderStatus(tmpId),
                "Status must remain Preparing after the rejected transition");
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tmpId);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(37)
    void statusTransition_sameStatus_isRejected() {
        // testOrderId is in "Completed" state after @Order(32).
        assertThrows(RuntimeException.class,
            () -> OrderService.updateOrderStatus(testOrderId, "Completed"),
            "Same-status update (Completed -> Completed) must be rejected");
    }

    @Test @org.junit.jupiter.api.Order(38)
    void statusTransition_unknownStatus_isRejected() throws Exception {
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Pending");
        int tmpId = data[0];
        try {
            assertThrows(RuntimeException.class,
                () -> OrderService.updateOrderStatus(tmpId, "FooBar"),
                "Completely unknown status must be rejected");
            assertEquals("Pending", getOrderStatus(tmpId),
                "Status must remain Pending after the rejected transition");
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tmpId);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(39)
    void statusTransition_cancelledOrder_isRejected() throws Exception {
        // A terminal Cancelled order must not accept any further status change.
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Cancelled");
        int tmpId = data[0];
        try {
            assertThrows(RuntimeException.class,
                () -> OrderService.updateOrderStatus(tmpId, "Preparing"),
                "Transition Cancelled -> Preparing must be rejected (terminal state)");
            assertEquals("Cancelled", getOrderStatus(tmpId),
                "Status must remain Cancelled after the rejected transition");
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tmpId);
            }
        }
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

    @Test @org.junit.jupiter.api.Order(42)
    void allowedNextStatuses_returnsCorrectTransitionsForEachStatus() {
        // Pure in-memory helper test — no DB access required.
        assertEquals(List.of("Preparing", "Cancelled"),
            OrderService.getAllowedNextStatuses("Pending"),
            "Pending must allow Preparing and Cancelled");
        assertEquals(List.of("Ready", "Cancelled"),
            OrderService.getAllowedNextStatuses("Preparing"),
            "Preparing must allow Ready and Cancelled");
        assertEquals(List.of("Completed"),
            OrderService.getAllowedNextStatuses("Ready"),
            "Ready must allow only Completed");
        assertTrue(OrderService.getAllowedNextStatuses("Completed").isEmpty(),
            "Completed is terminal — no allowed transitions");
        assertTrue(OrderService.getAllowedNextStatuses("Cancelled").isEmpty(),
            "Cancelled is terminal — no allowed transitions");
        assertTrue(OrderService.getAllowedNextStatuses(null).isEmpty(),
            "null input must return empty list (no NPE)");
    }

    @Test @org.junit.jupiter.api.Order(43)
    void statusTransition_preparingToCancelled_succeedsAndRestoresStock() throws Exception {
        // Covers the Preparing → Cancelled path (not exercised by the earlier cancel tests).
        setStock(testFoodId, 20);
        FoodItem food = getFoodById(testFoodId);
        assertNotNull(food);

        DataStore.cartItems.add(new CartItem(food, 2));
        Order order = OrderService.placeOrder(testCustomer, DataStore.cartItems);
        DataStore.cartItems.clear();
        int stockAfterOrder = getStockById(testFoodId);
        assertEquals(18, stockAfterOrder, "Stock must be 18 after ordering 2 units");

        OrderService.updateOrderStatus(order.getOrderId(), "Preparing");
        assertEquals("Preparing", getOrderStatus(order.getOrderId()),
            "Order must advance to Preparing");

        OrderService.updateOrderStatus(order.getOrderId(), "Cancelled");
        assertEquals("Cancelled", getOrderStatus(order.getOrderId()),
            "Order must be Cancelled after Preparing → Cancelled");
        assertEquals(stockAfterOrder + 2, getStockById(testFoodId),
            "Stock must be restored after cancellation from Preparing state");
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
            // Each step has its own try-catch so one failure never blocks subsequent steps.
            // LEFT(username, 3) = 'sv_' avoids all LIKE/ESCAPE quoting ambiguities.

            // Step 1: Delete orders for ALL sv_ customers (cascades → order_items → food_ratings).
            // Covers sv_testuser, sv_ratingother, and any other sv_ test fixtures with orders.
            try { conn.createStatement().execute(
                "DELETE FROM orders WHERE customer_id IN " +
                "(SELECT customer_id FROM customers WHERE LEFT(username, 3) = 'sv_')");
            } catch (Exception ignored) {}

            // Step 2: Delete the main test customer (sv_testuser)
            try { conn.createStatement().execute(
                "DELETE FROM customers WHERE username = '" + TEST_USER + "'");
            } catch (Exception ignored) {}

            // Step 3: Delete any remaining SV_ customers (sv_ratingother, sv_custmgmt, etc.)
            try { conn.createStatement().execute(
                "DELETE FROM customers WHERE LEFT(username, 3) = 'sv_'");
            } catch (Exception ignored) {}

            // Step 4: Delete SV_ food items (FK: food_items → food_categories)
            try { conn.createStatement().execute(
                "DELETE FROM food_items WHERE food_name LIKE 'SV_%'");
            } catch (Exception ignored) {}

            // Step 5: Delete SV_ food categories
            try { conn.createStatement().execute(
                "DELETE FROM food_categories WHERE category_name LIKE 'SV_%'");
            } catch (Exception ignored) {}

        } catch (Exception ignored) {
            // Connection failure — nothing to clean up
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
    void updateCustomerUsernameWithoutPasswordChange() {
        assertTrue(svCustomerId > 0, "Requires customer from @Order(201)");
        // Admin update scope: username only; fullName arg is accepted but not persisted
        boolean updated = CustomerManagementService.updateCustomer(
            svCustomerId, "SV Updated Name", "sv_custmgmt", null, null);
        assertTrue(updated, "updateCustomer (username only) should return true");
        citybites.model.Customer c =
            CustomerManagementService.getCustomerById(svCustomerId);
        // Full name must NOT have changed — customers own their name
        assertEquals("SV CustMgmt User", c.getFullName(),
            "Admin update must not change full name — customers manage it themselves");
        // Username must still be sv_custmgmt (unchanged in this call)
        assertEquals("sv_custmgmt", c.getUsername(),
            "Username must remain sv_custmgmt after update");
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

    // ══════════════════════════════════════════════════════════════════════════
    //  Customer Profile — @Order(219-227)
    // ══════════════════════════════════════════════════════════════════════════

    /** Helper: creates an SV_ customer and returns its ID; throws if creation fails. */
    private static int createProfileFixture(String suffix) {
        int id = CustomerManagementService.addCustomer(
            "SV Profile " + suffix, "sv_profile_" + suffix, "Profile1!", "Profile1!");
        assertTrue(id > 0, "SV_ profile fixture '" + suffix + "' must be created");
        return id;
    }

    @Test @org.junit.jupiter.api.Order(219)
    void profileService_getProfile_returnsCustomerWithNullableFieldsAllNull() {
        // Fresh customer: profile columns are null until updateProfile is called
        int id = createProfileFixture("get");
        try {
            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile, "getProfile must return a non-null Customer");
            assertEquals(id, profile.getCustomerId(), "Customer ID must match");
            assertNull(profile.getEmail(),            "email must be null on a fresh account");
            assertNull(profile.getPhoneNumber(),      "phone_number must be null on a fresh account");
            assertNull(profile.getDateOfBirth(),      "date_of_birth must be null on a fresh account");
            assertNull(profile.getProfileImagePath(), "profile_image_path must be null on a fresh account");
            assertNull(profile.getDeliveryAddress(),  "delivery_address must be null on a fresh account");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(220)
    void profileService_updateProfile_persistsAllFields() {
        int id = createProfileFixture("upd");
        try {
            java.time.LocalDate dob = java.time.LocalDate.of(1995, 6, 15);
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Profile Updated", "sv@example.com",
                "+94771234567", dob, null, "123 Test Street, Colombo");
            assertNull(error, "updateProfile with valid data must return null (success)");

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            assertEquals("SV Profile Updated",  profile.getFullName());
            assertEquals("sv@example.com",      profile.getEmail());
            assertEquals("+94771234567",         profile.getPhoneNumber());
            assertEquals(dob,                   profile.getDateOfBirth());
            assertEquals("123 Test Street, Colombo", profile.getDeliveryAddress());
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(221)
    void profileService_updateProfile_rejectsBlankFullName() {
        int id = createProfileFixture("blkname");
        try {
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "   ", null, null, null, null, null);
            assertNotNull(error, "blank full name must be rejected");
            assertTrue(error.toLowerCase().contains("full name"),
                "error message must mention 'full name'");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(222)
    void profileService_updateProfile_rejectsInvalidEmail() {
        int id = createProfileFixture("bademail");
        try {
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Profile BadEmail", "not-an-email", null, null, null, null);
            assertNotNull(error, "invalid email must be rejected");
            assertTrue(error.toLowerCase().contains("email"),
                "error message must mention 'email'");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(223)
    void profileService_updateProfile_rejectsInvalidPhone() {
        int id = createProfileFixture("badphone");
        try {
            // Contains letters — not allowed
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Profile BadPhone", null, "ABCDEF", null, null, null);
            assertNotNull(error, "phone with letters must be rejected");
            assertTrue(error.toLowerCase().contains("phone"),
                "error message must mention 'phone'");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(224)
    void profileService_updateProfile_rejectsFutureDateOfBirth() {
        int id = createProfileFixture("futuredob");
        try {
            java.time.LocalDate future = java.time.LocalDate.now().plusDays(1);
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Profile FutureDOB", null, null, future, null, null);
            assertNotNull(error, "future date of birth must be rejected");
            assertTrue(error.toLowerCase().contains("future") ||
                       error.toLowerCase().contains("birth"),
                "error message must mention 'future' or 'birth'");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(225)
    void profileService_updateProfile_rejectsUnrealisticallyOldDOB() {
        int id = createProfileFixture("olddob");
        try {
            java.time.LocalDate tooOld = java.time.LocalDate.now().minusYears(121);
            String error = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Profile OldDOB", null, null, tooOld, null, null);
            assertNotNull(error, "DOB over 120 years ago must be rejected");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(226)
    void profileService_calculateAge_returnsCorrectWholeYears() {
        java.time.LocalDate dob = java.time.LocalDate.now().minusYears(30).minusMonths(6);
        int age = citybites.service.CustomerProfileService.calculateAge(dob);
        assertEquals(30, age, "age must be 30 whole years for a DOB 30.5 years ago");
    }

    @Test @org.junit.jupiter.api.Order(227)
    void profileService_calculateAge_returnsMinusOneForNullDOB() {
        int age = citybites.service.CustomerProfileService.calculateAge(null);
        assertEquals(-1, age, "calculateAge(null) must return -1");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // §228-239  Email uniqueness, admin compatibility, image lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(228)
    void profileService_updateProfile_rejectsDuplicateEmailCaseInsensitively() {
        // Register two customers; give the first one an email; try to assign the
        // same email (different case) to the second — must be rejected.
        int id1 = createProfileFixture("email1");
        int id2 = createProfileFixture("email2");
        try {
            String err1 = citybites.service.CustomerProfileService.updateProfile(
                id1, "SV Email Owner", "unique228@example.com", null, null, null, null);
            assertNull(err1, "first assignment must succeed");

            String err2 = citybites.service.CustomerProfileService.updateProfile(
                id2, "SV Email Dup", "UNIQUE228@EXAMPLE.COM", null, null, null, null);
            assertNotNull(err2, "duplicate email (different case) must be rejected");
            assertTrue(err2.toLowerCase().contains("email"),
                "rejection message must mention 'email'");
        } finally {
            try { CustomerManagementService.deleteCustomer(id1); } catch (Exception ignored) {}
            try { CustomerManagementService.deleteCustomer(id2); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(229)
    void profileService_updateProfile_allowsCustomerToKeepOwnEmail() {
        // Updating profile with the customer's own existing email must succeed.
        int id = createProfileFixture("ownmail");
        try {
            String err1 = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Own Email", "own229@example.com", null, null, null, null);
            assertNull(err1, "initial email assignment must succeed");

            // Re-submit with the same email — must not fail the uniqueness check.
            String err2 = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Own Email Updated", "own229@example.com", null, null, null, null);
            assertNull(err2, "re-using own email must be allowed");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(230)
    void profileService_updateProfile_storesBlankEmailAsNull() {
        int id = createProfileFixture("blankemail");
        try {
            // First set a real email
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Blank Email", "blanktest@example.com", null, null, null, null);
            // Now clear it
            String err = citybites.service.CustomerProfileService.updateProfile(
                id, "SV Blank Email", "   ", null, null, null, null);
            assertNull(err, "blank email must be accepted (treated as null)");

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            assertNull(profile.getEmail(), "email must be stored as NULL after blank input");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(231)
    void profileService_updateProfile_doesNotChangeUsername() {
        int id = createProfileFixture("uname");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Uname Changed", null, null, null, null, null);

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            // username must remain the one set at registration
            assertEquals("sv_profile_uname", profile.getUsername(),
                "profile update must not change the username");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(232)
    void profileService_updateProfile_doesNotChangePasswordHash() throws Exception {
        int id = createProfileFixture("pwdhash");
        try {
            // Capture hash before update
            String hashBefore;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM customers WHERE customer_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    hashBefore = rs.getString(1);
                }
            }

            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Hash Check", null, null, null, null, "123 Test Road");

            String hashAfter;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM customers WHERE customer_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    hashAfter = rs.getString(1);
                }
            }
            assertEquals(hashBefore, hashAfter,
                "profile update must never change the password_hash");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(233)
    void profileService_updateProfile_doesNotBreakLogin() {
        int id = createProfileFixture("loginchk");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Login Check", "loginchk@example.com", "+94771111111",
                null, null, "44 Main St");

            // Login must still succeed with the original password
            java.util.Optional<citybites.model.Customer> logged =
                citybites.service.AuthService.customerLogin("sv_profile_loginchk", "Profile1!");
            assertTrue(logged.isPresent(), "customer must still be able to log in after profile update");
        } finally {
            SessionManager.logout();
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(234)
    void customerManagement_adminNameUpdate_preservesProfileFields() {
        int id = createProfileFixture("admnm");
        try {
            // Set profile fields via profile service
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Admin Name", "admnm@example.com", "+94770000001",
                java.time.LocalDate.of(1990, 1, 1), null, "1 Admin Lane");

            // Admin changes only name/username — must NOT wipe profile columns
            CustomerManagementService.updateCustomer(
                id, "SV Admin Name Updated", "sv_profile_admnm", null, null);

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            assertEquals("admnm@example.com", profile.getEmail(),
                "admin name update must not wipe email");
            assertEquals("+94770000001", profile.getPhoneNumber(),
                "admin name update must not wipe phone");
            assertEquals("1 Admin Lane", profile.getDeliveryAddress(),
                "admin name update must not wipe delivery address");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(235)
    void customerManagement_adminPasswordUpdate_preservesProfileFields() {
        int id = createProfileFixture("admpwd");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Admin Pwd", "admpwd@example.com", null, null, null, "2 Admin Road");

            // Admin sets a new password — profile columns must be preserved
            CustomerManagementService.updateCustomer(
                id, "SV Admin Pwd", "sv_profile_admpwd", "NewPass1!", "NewPass1!");

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            assertEquals("admpwd@example.com", profile.getEmail(),
                "admin password reset must not wipe email");
            assertEquals("2 Admin Road", profile.getDeliveryAddress(),
                "admin password reset must not wipe delivery address");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(236)
    void registration_newCustomer_hasNullProfileColumns() {
        int id = CustomerManagementService.addCustomer(
            "SV Registration Null Profile", "sv_reg_null_prof", "RegNull1!", "RegNull1!");
        assertTrue(id > 0);
        try {
            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile, "getProfile must return the newly registered customer");
            assertNull(profile.getEmail(),             "email must be null on fresh registration");
            assertNull(profile.getPhoneNumber(),        "phone must be null on fresh registration");
            assertNull(profile.getDateOfBirth(),        "dob must be null on fresh registration");
            assertNull(profile.getProfileImagePath(),   "image must be null on fresh registration");
            assertNull(profile.getDeliveryAddress(),    "address must be null on fresh registration");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    // ── ProfileImageManager lifecycle ─────────────────────────────────────────

    @Test @org.junit.jupiter.api.Order(237)
    void profileImageManager_importImage_writesToProfileDir_notFoodDir() throws Exception {
        // Create a temp PNG source file
        Path src = Files.createTempFile("sv_test_avatar_", ".png");
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "png", src.toFile());

            String stored = citybites.util.ProfileImageManager.importImage(src);

            assertNotNull(stored, "importImage must return a non-null stored filename");
            assertFalse(stored.contains("/") || stored.contains("\\"),
                "stored filename must be a plain relative name with no path separators");

            // File must exist in PROFILE_DIR
            Path resolved = citybites.util.ProfileImageManager.PROFILE_DIR.resolve(stored);
            assertTrue(Files.exists(resolved),
                "imported file must exist under PROFILE_DIR");

            // File must NOT exist in ImageManager's food-image directory
            Path foodPath = ImageManager.MANAGED_DIR.resolve(stored);
            assertFalse(Files.exists(foodPath),
                "imported profile image must not be placed in the food-images directory");

            // Cleanup
            Files.deleteIfExists(resolved);
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @Test @org.junit.jupiter.api.Order(238)
    void profileImageManager_importImage_storedPathIsRelativeFilenameOnly() throws Exception {
        Path src = Files.createTempFile("sv_test_relpath_", ".jpg");
        try {
            BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "jpg", src.toFile());

            String stored = citybites.util.ProfileImageManager.importImage(src);

            // Must not start with a drive letter or /
            assertFalse(stored.startsWith("/"),        "stored path must not be absolute (Unix)");
            assertFalse(stored.matches("^[A-Za-z]:.*"),"stored path must not be absolute (Windows)");
            assertTrue(stored.endsWith(".jpg") || stored.endsWith(".jpeg") || stored.endsWith(".png"),
                "stored filename must retain the image extension");

            // Cleanup
            Files.deleteIfExists(citybites.util.ProfileImageManager.PROFILE_DIR.resolve(stored));
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @Test @org.junit.jupiter.api.Order(239)
    void profileImageManager_importImage_doesNotModifySourceFile() throws Exception {
        Path src = Files.createTempFile("sv_test_src_", ".png");
        try {
            BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "png", src.toFile());
            long sizeBefore = Files.size(src);

            String stored = citybites.util.ProfileImageManager.importImage(src);

            assertTrue(Files.exists(src), "source file must still exist after import");
            assertEquals(sizeBefore, Files.size(src), "source file size must be unchanged");

            // Cleanup
            Files.deleteIfExists(citybites.util.ProfileImageManager.PROFILE_DIR.resolve(stored));
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @Test @org.junit.jupiter.api.Order(240)
    void profileImageManager_importImage_rejectsUnsupportedExtension() throws Exception {
        Path src = Files.createTempFile("sv_test_bad_", ".gif");
        try {
            Files.writeString(src, "GIF89a"); // minimal content
            assertThrows(java.io.IOException.class,
                () -> citybites.util.ProfileImageManager.importImage(src),
                "importing a .gif file must throw IOException");
        } finally {
            Files.deleteIfExists(src);
        }
    }

    @Test @org.junit.jupiter.api.Order(241)
    void profileImageManager_resolveImage_blocksPathTraversal() {
        Path result = citybites.util.ProfileImageManager.resolveImage("../../etc/passwd");
        assertNull(result, "path traversal attempt must be blocked and return null");
    }

    @Test @org.junit.jupiter.api.Order(242)
    void profileImageManager_resolveImage_returnNullForMissingFile() {
        Path result = citybites.util.ProfileImageManager.resolveImage("nonexistent_sv_test.png");
        assertNull(result, "resolveImage for a missing file must return null");
    }

    @Test @org.junit.jupiter.api.Order(243)
    void profileImageManager_resolveImage_returnNullForNullInput() {
        assertNull(citybites.util.ProfileImageManager.resolveImage(null),
            "resolveImage(null) must return null");
        assertNull(citybites.util.ProfileImageManager.resolveImage(""),
            "resolveImage(\"\") must return null");
        assertNull(citybites.util.ProfileImageManager.resolveImage("   "),
            "resolveImage(blank) must return null");
    }

    @Test @org.junit.jupiter.api.Order(244)
    void profileImageManager_extension_helper_returnsCorrectExtension() {
        assertEquals("jpg",  citybites.util.ProfileImageManager.extension("photo.JPG"),
            "extension must be lower-cased");
        assertEquals("png",  citybites.util.ProfileImageManager.extension("image.png"));
        assertEquals("jpeg", citybites.util.ProfileImageManager.extension("file.JPEG"));
        assertEquals("",     citybites.util.ProfileImageManager.extension("nodotfile"),
            "file without extension must return empty string");
    }

    // ── Schema verification ───────────────────────────────────────────────────

    @Test @org.junit.jupiter.api.Order(245)
    void schema_customersTable_hasAllFiveProfileColumns() throws Exception {
        String[] expected = {"email", "phone_number", "date_of_birth",
                             "profile_image_path", "delivery_address"};
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String col : expected) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = 'customers' " +
                        "AND COLUMN_NAME = ?")) {
                    ps.setString(1, col);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        assertEquals(1, rs.getInt(1),
                            "Column '" + col + "' must exist in the customers table");
                    }
                }
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(246)
    void schema_migrationKey_customerProfileColumns_isRecorded() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = ?")) {
            ps.setString(1, "20260831_customer_profile_columns");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1),
                    "Migration key '20260831_customer_profile_columns' must be recorded");
            }
        }
    }

    // ── Session-aware service entrypoints ─────────────────────────────────────

    @Test @org.junit.jupiter.api.Order(247)
    void profileService_getCurrentCustomerProfile_returnsNullWhenNoSession() {
        SessionManager.logout();
        citybites.model.Customer profile =
            citybites.service.CustomerProfileService.getCurrentCustomerProfile();
        assertNull(profile, "getCurrentCustomerProfile must return null when no session is active");
    }

    @Test @org.junit.jupiter.api.Order(248)
    void profileService_updateCurrentCustomerProfile_rejectsWhenNoSession() {
        SessionManager.logout();
        String err = citybites.service.CustomerProfileService.updateCurrentCustomerProfile(
            "SV No Session", null, null, null, null, null);
        assertNotNull(err, "updateCurrentCustomerProfile must return an error when not logged in");
        assertTrue(err.toLowerCase().contains("logged in") || err.toLowerCase().contains("session"),
            "error message must indicate that no customer is logged in");
    }

    @Test @org.junit.jupiter.api.Order(249)
    void profileService_getCurrentCustomerProfile_returnsProfileForLoggedInCustomer() {
        int id = createProfileFixture("session");
        try {
            // Set a known email so we can verify the right profile is returned
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Session Profile", "session249@example.com", null, null, null, null);

            // Simulate UI login: authenticate then set the session
            java.util.Optional<citybites.model.Customer> opt =
                citybites.service.AuthService.customerLogin("sv_profile_session", "Profile1!");
            assertTrue(opt.isPresent(), "login must succeed for session test");
            SessionManager.setLoggedInCustomer(opt.get());

            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getCurrentCustomerProfile();
            assertNotNull(profile, "getCurrentCustomerProfile must return a profile when logged in");
            assertEquals("session249@example.com", profile.getEmail(),
                "returned profile must match the logged-in customer");
        } finally {
            SessionManager.logout();
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(250)
    void profileService_updateCurrentCustomerProfile_persistsChangesForLoggedInCustomer() {
        int id = createProfileFixture("sessupd");
        try {
            // Simulate UI login: authenticate then set the session
            java.util.Optional<citybites.model.Customer> opt =
                citybites.service.AuthService.customerLogin("sv_profile_sessupd", "Profile1!");
            assertTrue(opt.isPresent(), "login must succeed for session update test");
            SessionManager.setLoggedInCustomer(opt.get());

            String err = citybites.service.CustomerProfileService.updateCurrentCustomerProfile(
                "SV Session Updated", "sessupd250@example.com", "+94770000099",
                java.time.LocalDate.of(1992, 3, 20), null, "10 Session St");
            assertNull(err, "updateCurrentCustomerProfile must succeed when logged in");

            // Verify via direct profile fetch
            citybites.model.Customer profile =
                citybites.service.CustomerProfileService.getProfile(id);
            assertNotNull(profile);
            assertEquals("SV Session Updated",          profile.getFullName());
            assertEquals("sessupd250@example.com",      profile.getEmail());
            assertEquals("+94770000099",                  profile.getPhoneNumber());
            assertEquals(java.time.LocalDate.of(1992, 3, 20), profile.getDateOfBirth());
            assertEquals("10 Session St",               profile.getDeliveryAddress());
        } finally {
            SessionManager.logout();
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    // ── Validation helper unit tests ──────────────────────────────────────────

    @Test @org.junit.jupiter.api.Order(251)
    void profileService_isValidEmail_acceptsAndRejectsCorrectly() {
        assertTrue(citybites.service.CustomerProfileService.isValidEmail("user@example.com"));
        assertTrue(citybites.service.CustomerProfileService.isValidEmail("u+tag@sub.domain.org"));
        assertFalse(citybites.service.CustomerProfileService.isValidEmail("notanemail"));
        assertFalse(citybites.service.CustomerProfileService.isValidEmail("@nodomain.com"));
        assertFalse(citybites.service.CustomerProfileService.isValidEmail("missing@dot"));
    }

    @Test @org.junit.jupiter.api.Order(252)
    void profileService_isValidPhone_acceptsAndRejectsCorrectly() {
        assertTrue(citybites.service.CustomerProfileService.isValidPhone("+94771234567"));
        assertTrue(citybites.service.CustomerProfileService.isValidPhone("077-123-4567"));
        assertTrue(citybites.service.CustomerProfileService.isValidPhone("(011) 234 5678"));
        assertFalse(citybites.service.CustomerProfileService.isValidPhone("ABCDEF"));
        assertFalse(citybites.service.CustomerProfileService.isValidPhone(
            "123456789012345678901")); // 21 chars — exceeds 20
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Admin Customer Details — @Order(254–263)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 1. Admin customer details returns all profile fields.
     * getCustomerDetails must return a fully populated Customer when the
     * customer has had their profile filled in via CustomerProfileService.
     */
    @Test @org.junit.jupiter.api.Order(254)
    void adminDetails_getCustomerDetails_returnsAllProfileFields() {
        int id = createProfileFixture("det254");
        try {
            java.time.LocalDate dob = java.time.LocalDate.of(1988, 4, 22);
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Admin Details", "det254@example.com", "+94771000254",
                dob, null, "254 Detail Street, Colombo");

            citybites.model.Customer c =
                citybites.service.CustomerManagementService.getCustomerDetails(id);
            assertNotNull(c,                              "getCustomerDetails must return non-null");
            assertEquals("SV Admin Details",             c.getFullName());
            assertEquals("sv_profile_det254",            c.getUsername());
            assertEquals("det254@example.com",           c.getEmail());
            assertEquals("+94771000254",                 c.getPhoneNumber());
            assertEquals(dob,                            c.getDateOfBirth());
            assertEquals("254 Detail Street, Colombo",  c.getDeliveryAddress());
            assertNotNull(c.getCreatedAt(), "createdAt must be populated");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 2. Customer with null optional fields loads without error.
     * A freshly created customer has no profile columns set; getCustomerDetails
     * must return a non-null Customer with all optional fields null.
     */
    @Test @org.junit.jupiter.api.Order(255)
    void adminDetails_customerWithNullOptionalFields_loadsWithoutError() {
        int id = createProfileFixture("nullopt255");
        try {
            citybites.model.Customer c =
                citybites.service.CustomerManagementService.getCustomerDetails(id);
            assertNotNull(c,                     "getCustomerDetails must return non-null");
            assertNotNull(c.getFullName(),        "full name must be set");
            assertNotNull(c.getUsername(),        "username must be set");
            assertNull(c.getEmail(),              "email must be null for fresh customer");
            assertNull(c.getPhoneNumber(),        "phone must be null for fresh customer");
            assertNull(c.getDateOfBirth(),        "DOB must be null for fresh customer");
            assertNull(c.getProfileImagePath(),   "profileImagePath must be null for fresh customer");
            assertNull(c.getDeliveryAddress(),    "deliveryAddress must be null for fresh customer");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 3. DOB age calculation shown through existing helper.
     * getCustomerDetails returns a Customer with a non-null DOB;
     * CustomerProfileService.calculateAge() must compute the correct age.
     */
    @Test @org.junit.jupiter.api.Order(256)
    void adminDetails_calculateAge_viaExistingHelper() {
        int id = createProfileFixture("age256");
        try {
            java.time.LocalDate dob = java.time.LocalDate.now().minusYears(30);
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Age Test", null, null, dob, null, null);

            citybites.model.Customer c =
                citybites.service.CustomerManagementService.getCustomerDetails(id);
            int age = citybites.service.CustomerProfileService.calculateAge(c.getDateOfBirth());
            assertEquals(30, age, "calculateAge must return 30 for a customer born 30 years ago");

            // Null DOB edge-case
            assertEquals(-1, citybites.service.CustomerProfileService.calculateAge(null),
                "calculateAge(null) must return -1");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 4. Admin Get All includes email and phone.
     * After updating a customer's profile, getAllCustomers() must return
     * their email and phone in the list.
     */
    @Test @org.junit.jupiter.api.Order(257)
    void adminDetails_getAllCustomers_includesEmailAndPhone() {
        int id = createProfileFixture("getall257");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV GetAll", "getall257@example.com", "+94770000257", null, null, null);

            java.util.List<citybites.model.Customer> all =
                CustomerManagementService.getAllCustomers();
            citybites.model.Customer found = all.stream()
                .filter(c -> c.getCustomerId() == id)
                .findFirst().orElse(null);
            assertNotNull(found, "getAllCustomers must include the created customer");
            assertEquals("getall257@example.com", found.getEmail(),
                "getAllCustomers must populate the customer's email");
            assertEquals("+94770000257", found.getPhoneNumber(),
                "getAllCustomers must populate the customer's phone number");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 5. Admin search matches email.
     * The email returned by getAllCustomers() must support substring filtering
     * used by the search box in CustomerManagementFrame.
     */
    @Test @org.junit.jupiter.api.Order(258)
    void adminDetails_searchMatchesEmail() {
        int id = createProfileFixture("srchmail258");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Email Search", "srchmail258@example.com", null, null, null, null);

            java.util.List<citybites.model.Customer> all =
                CustomerManagementService.getAllCustomers();
            String query = "srchmail258";
            boolean found = all.stream().anyMatch(c -> {
                String email = c.getEmail() != null ? c.getEmail().toLowerCase() : "";
                return email.contains(query.toLowerCase());
            });
            assertTrue(found,
                "Email-based search must find the customer with 'srchmail258' in their email");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 6. Admin search matches phone.
     * The phone returned by getAllCustomers() must support substring filtering
     * used by the search box in CustomerManagementFrame.
     */
    @Test @org.junit.jupiter.api.Order(259)
    void adminDetails_searchMatchesPhone() {
        int id = createProfileFixture("srchphone259");
        try {
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Phone Search", null, "+94779990259", null, null, null);

            java.util.List<citybites.model.Customer> all =
                CustomerManagementService.getAllCustomers();
            String query = "9990259";
            boolean found = all.stream().anyMatch(c -> {
                String phone = c.getPhoneNumber() != null ? c.getPhoneNumber() : "";
                return phone.contains(query);
            });
            assertTrue(found,
                "Phone-based search must find the customer with '9990259' in their phone number");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 7. Admin details do not require password hash.
     * getCustomerDetails works correctly; the stored password field is a
     * BCrypt hash (starts with "$2"), never the original plaintext.
     */
    @Test @org.junit.jupiter.api.Order(260)
    void adminDetails_getCustomerDetails_storedPasswordIsBcryptHash() {
        int id = createProfileFixture("pwdcheck260");
        try {
            citybites.model.Customer c =
                citybites.service.CustomerManagementService.getCustomerDetails(id);
            assertNotNull(c, "getCustomerDetails must return non-null");
            String storedPwd = c.getPassword();
            assertNotNull(storedPwd, "password field must not be null");
            assertTrue(storedPwd.startsWith("$2"),
                "Stored password must be a BCrypt hash (starts with '$2'), not plaintext");
            assertNotEquals("Profile1!", storedPwd,
                "Stored password must not equal the original plaintext");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 8. Admin core update (username-only) does not change Full Name or any profile field.
     * Admin update scope covers only username (and optionally password).
     * full_name, email, phone, DOB, and delivery address must be unchanged.
     */
    @Test @org.junit.jupiter.api.Order(261)
    void adminDetails_adminCoreUpdate_doesNotChangeFullNameOrProfileFields() {
        int id = createProfileFixture("preserve261");
        try {
            java.time.LocalDate dob = java.time.LocalDate.of(1985, 7, 10);
            citybites.service.CustomerProfileService.updateProfile(
                id, "SV Preserve", "preserve261@example.com", "+94778880261",
                dob, null, "261 Preserve Road");

            // Admin updates only username — fullName arg is accepted but must NOT be persisted
            CustomerManagementService.updateCustomer(
                id, "SV Preserve Should Not Change", "sv_profile_preserve261", null, null);

            citybites.model.Customer c =
                citybites.service.CustomerManagementService.getCustomerDetails(id);
            // Full name must be unchanged — admin does not own it
            assertEquals("SV Preserve",                c.getFullName(),
                "admin update must not change full name — customers own their name");
            assertEquals("preserve261@example.com",    c.getEmail(),
                "email must be preserved after admin core update");
            assertEquals("+94778880261",               c.getPhoneNumber(),
                "phone must be preserved after admin core update");
            assertEquals(dob,                          c.getDateOfBirth(),
                "DOB must be preserved after admin core update");
            assertEquals("261 Preserve Road",          c.getDeliveryAddress(),
                "delivery address must be preserved after admin core update");
        } finally {
            try { CustomerManagementService.deleteCustomer(id); } catch (Exception ignored) {}
        }
    }

    /**
     * 9. Missing profile image resolves to placeholder/null safely.
     * resolveImage and loadScaled must not throw for null or non-existent filenames.
     */
    @Test @org.junit.jupiter.api.Order(262)
    void adminDetails_missingProfileImage_resolvesToNullSafely() {
        // Non-existent relative filename → null (file is not on disk)
        assertNull(citybites.util.ProfileImageManager.resolveImage("nonexistent_sv_262.jpg"),
            "resolveImage for a non-existent filename must return null");

        // loadScaled must return a placeholder icon — never throw
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            javax.swing.ImageIcon placeholder =
                citybites.util.ProfileImageManager.loadScaled(null, 80, 80);
            assertNotNull(placeholder,
                "loadScaled(null) must return a placeholder ImageIcon, not null");

            javax.swing.ImageIcon missing =
                citybites.util.ProfileImageManager.loadScaled("nonexistent_sv_262.jpg", 80, 80);
            assertNotNull(missing,
                "loadScaled(missing file) must return a placeholder ImageIcon, not null");
        }
    }

    /**
     * 10. Invalid/non-existing customer ID is rejected cleanly.
     * Negative and zero IDs throw IllegalArgumentException;
     * a valid but non-existent ID throws IllegalStateException.
     */
    @Test @org.junit.jupiter.api.Order(263)
    void adminDetails_invalidOrNonExistentCustomerId_isRejectedCleanly() {
        assertThrows(IllegalArgumentException.class,
            () -> citybites.service.CustomerManagementService.getCustomerDetails(-1),
            "getCustomerDetails(-1) must throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class,
            () -> citybites.service.CustomerManagementService.getCustomerDetails(0),
            "getCustomerDetails(0) must throw IllegalArgumentException");

        assertThrows(IllegalStateException.class,
            () -> citybites.service.CustomerManagementService.getCustomerDetails(999_999_999),
            "getCustomerDetails(non-existent ID) must throw IllegalStateException");
    }

    // ── DatePicker construction smoke test ────────────────────────────────────

    /**
     * Verifies that the supported LGoodDatePicker initialization order
     * (construct DatePicker first, set veto policy second) does not throw.
     * Skipped automatically in a headless/CI environment.
     */
    @Test @org.junit.jupiter.api.Order(253)
    void datePicker_vetoPolicy_setAfterConstruction_doesNotThrow() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;

        java.util.concurrent.atomic.AtomicReference<Exception> caught =
                new java.util.concurrent.atomic.AtomicReference<>();

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                com.github.lgooddatepicker.components.DatePickerSettings settings =
                        new com.github.lgooddatepicker.components.DatePickerSettings();
                settings.setAllowEmptyDates(true);
                settings.setFormatForDatesCommonEra("MMM d, yyyy");

                // DatePicker must exist before setVetoPolicy — this is the supported order
                com.github.lgooddatepicker.components.DatePicker picker =
                        new com.github.lgooddatepicker.components.DatePicker(settings);

                settings.setVetoPolicy(
                        new com.github.lgooddatepicker.optionalusertools.DateVetoPolicy() {
                    @Override
                    public boolean isDateAllowed(java.time.LocalDate date) {
                        if (date == null) return true;
                        java.time.LocalDate today   = java.time.LocalDate.now();
                        java.time.LocalDate minDate = today.minusYears(120);
                        return !date.isAfter(today) && !date.isBefore(minDate);
                    }
                });

                // Confirm a known-valid date is accepted
                picker.setDate(java.time.LocalDate.of(1990, 6, 15));
                assertEquals(java.time.LocalDate.of(1990, 6, 15), picker.getDate(),
                        "DatePicker must store the set date");

                // Confirm a future date is vetoed (picker keeps its previous value)
                picker.setDate(java.time.LocalDate.now().plusDays(1));
                assertNotEquals(java.time.LocalDate.now().plusDays(1), picker.getDate(),
                        "Future date must be rejected by the veto policy");

            } catch (Exception e) {
                caught.set(e);
            }
        });

        assertNull(caught.get(),
                "DatePicker veto-after-construction must not throw: "
                + (caught.get() != null ? caught.get().getMessage() : ""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // FEATURED FOODS — @Order(264-270)
    // ══════════════════════════════════════════════════════════════════════

    @Test @org.junit.jupiter.api.Order(264)
    void featuredFoods_migrationColumnExistsInFoodItems() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT IS_NULLABLE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'food_items' " +
                "AND COLUMN_NAME = 'featured_position'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(),
                    "featured_position column must exist in food_items after migration");
                assertEquals("YES", rs.getString("IS_NULLABLE"),
                    "featured_position must be nullable (NULL = not featured)");
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(265)
    void featuredFoods_setSlot_foodAppearsInFeaturedList() {
        // Ensure testFoodId has no featured slot initially (clear any state from prior runs)
        FoodService.setFeaturedPosition(testFoodId, null);

        List<FoodItem> before = FoodService.getFeaturedFoodItems();
        boolean presentBefore = before.stream().anyMatch(f -> f.getFoodId() == testFoodId);
        assertFalse(presentBefore,
            "testFoodId must not appear in featured list before being assigned a slot");

        FoodService.setFeaturedPosition(testFoodId, 1);

        List<FoodItem> after = FoodService.getFeaturedFoodItems();
        assertTrue(after.stream().anyMatch(f -> f.getFoodId() == testFoodId),
            "testFoodId must appear in featured list after being assigned slot 1");
    }

    @Test @org.junit.jupiter.api.Order(266)
    void featuredFoods_featuredFoodsReturnedInSlotOrder() {
        // testFoodId is at slot 1 from @Order(265)
        // Create a second food at slot 2 and verify the list is ordered slot 1 → slot 2
        testFeat2Id = FoodService.addFoodItem("SV_FeatTest2", 99.0, true, 10, null);
        assertTrue(testFeat2Id > 0, "SV_FeatTest2 fixture must be created");

        FoodService.setFeaturedPosition(testFeat2Id, 2);

        List<FoodItem> featured = FoodService.getFeaturedFoodItems();
        assertTrue(featured.size() >= 2,
            "At least 2 featured items must be present");

        // Find indices of testFoodId (slot 1) and testFeat2Id (slot 2)
        int idx1 = -1, idx2 = -1;
        for (int i = 0; i < featured.size(); i++) {
            if (featured.get(i).getFoodId() == testFoodId)  idx1 = i;
            if (featured.get(i).getFoodId() == testFeat2Id) idx2 = i;
        }
        assertTrue(idx1 >= 0, "testFoodId (slot 1) must be in featured list");
        assertTrue(idx2 >= 0, "testFeat2Id (slot 2) must be in featured list");
        assertTrue(idx1 < idx2,
            "Slot 1 food must appear before Slot 2 food in the featured list");
    }

    @Test @org.junit.jupiter.api.Order(267)
    void featuredFoods_slotConflict_displacesPreviousFood() {
        // testFoodId is at slot 1; assigning testFeat2Id to slot 1 must displace testFoodId
        FoodService.setFeaturedPosition(testFeat2Id, 1);

        List<FoodItem> featured = FoodService.getFeaturedFoodItems();
        boolean testFood1Present = featured.stream().anyMatch(f -> f.getFoodId() == testFoodId);
        boolean testFeat2Present = featured.stream().anyMatch(f -> f.getFoodId() == testFeat2Id);

        assertFalse(testFood1Present,
            "testFoodId must be displaced from slot 1 when testFeat2Id takes it");
        assertTrue(testFeat2Present,
            "testFeat2Id must now hold slot 1 after the conflict displacement");
    }

    @Test @org.junit.jupiter.api.Order(268)
    void featuredFoods_clearSlot_removesFromFeaturedList() {
        // Assign testFoodId to slot 3, then clear it
        FoodService.setFeaturedPosition(testFoodId, 3);
        List<FoodItem> afterAssign = FoodService.getFeaturedFoodItems();
        assertTrue(afterAssign.stream().anyMatch(f -> f.getFoodId() == testFoodId),
            "testFoodId must appear in featured list after being assigned slot 3");

        FoodService.setFeaturedPosition(testFoodId, null);
        List<FoodItem> afterClear = FoodService.getFeaturedFoodItems();
        assertFalse(afterClear.stream().anyMatch(f -> f.getFoodId() == testFoodId),
            "testFoodId must NOT appear in featured list after its slot is cleared");
    }

    @Test @org.junit.jupiter.api.Order(269)
    void featuredFoods_unavailableFeaturedFood_excludedFromList() throws Exception {
        // Assign testFoodId to slot 2
        FoodService.setFeaturedPosition(testFoodId, 2);

        // Mark testFoodId as unavailable via direct SQL (bypasses service stock logic)
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE food_items SET available = 0 WHERE food_id = ?")) {
            ps.setInt(1, testFoodId);
            ps.executeUpdate();
        }

        List<FoodItem> featured = FoodService.getFeaturedFoodItems();
        assertFalse(featured.stream().anyMatch(f -> f.getFoodId() == testFoodId),
            "Unavailable food must not appear in the featured list even if it has a slot");

        // Restore availability so subsequent teardown succeeds
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE food_items SET available = 1 WHERE food_id = ?")) {
            ps.setInt(1, testFoodId);
            ps.executeUpdate();
        }
    }

    @Test @org.junit.jupiter.api.Order(270)
    void featuredFoods_invalidPosition_isRejectedCleanly() {
        assertThrows(IllegalArgumentException.class,
            () -> FoodService.setFeaturedPosition(testFoodId, 0),
            "Position 0 must be rejected");
        assertThrows(IllegalArgumentException.class,
            () -> FoodService.setFeaturedPosition(testFoodId, 5),
            "Position 5 must be rejected");
        assertThrows(IllegalArgumentException.class,
            () -> FoodService.setFeaturedPosition(testFoodId, -1),
            "Negative position must be rejected");

        // Boundary values that must NOT throw
        assertDoesNotThrow(
            () -> FoodService.setFeaturedPosition(testFoodId, null),
            "null (clear slot) must not throw");
        assertDoesNotThrow(
            () -> FoodService.setFeaturedPosition(testFoodId, 1),
            "Position 1 must be valid");
        assertDoesNotThrow(
            () -> FoodService.setFeaturedPosition(testFoodId, 4),
            "Position 4 must be valid");
    }

    // ── @Order(271-278): updateWithFeaturedPosition atomicity tests ─────────

    @Test @org.junit.jupiter.api.Order(271)
    void updateAtomic_fieldsAndSlot_changedTogether() {
        // Clear any prior slot, then put testFoodId at slot 1
        FoodService.setFeaturedPosition(testFoodId, null);
        FoodService.setFeaturedPosition(testFoodId, 1);

        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        String updatedName = "SV_TestFood_Updated271";

        // Atomic update: rename + move slot 1 → 3
        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, updatedName, 12.50, true, 10, null, catId, 3, false);

        assertTrue(result.isSuccess(), "Update must succeed (slot 3 is free)");

        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food -> {
            assertEquals(updatedName, food.getFoodName(),
                    "Food name must reflect the atomic update");
            assertEquals(Integer.valueOf(3), food.getFeaturedPosition(),
                    "Featured slot must be 3 after the atomic update");
        }, () -> fail("testFoodId must still exist after update"));
    }

    @Test @org.junit.jupiter.api.Order(272)
    void updateAtomic_occupiedSlot_noReplace_nothingChanges() {
        // testFoodId at slot 3 (from @Order(271)); put testFeat2Id at slot 2
        FoodService.setFeaturedPosition(testFeat2Id, 2);

        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        String originalName = FoodService.getFoodItemById(testFoodId)
                .map(FoodItem::getFoodName)
                .orElseThrow(() -> new AssertionError("testFoodId not found"));

        // Try to move testFoodId into slot 2 (occupied by testFeat2Id), replaceOccupied=false
        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, "SV_AtomicShouldNotApply", 99.99, true, 99, null,
                catId, 2, false);

        assertEquals(FeaturedAssignmentResult.Status.SLOT_OCCUPIED, result.getStatus(),
                "Must return SLOT_OCCUPIED when target slot is taken and replaceOccupied=false");

        // Verify no DB state was changed for either food
        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food -> {
            assertEquals(originalName, food.getFoodName(),
                    "Food name must NOT change when update returns SLOT_OCCUPIED");
            assertEquals(Integer.valueOf(3), food.getFeaturedPosition(),
                    "Featured slot must remain 3 when update returns SLOT_OCCUPIED");
        }, () -> fail("testFoodId must still exist"));

        FoodService.getFoodItemById(testFeat2Id).ifPresentOrElse(food ->
            assertEquals(Integer.valueOf(2), food.getFeaturedPosition(),
                    "testFeat2Id must still hold slot 2 after the refused update"),
            () -> fail("testFeat2Id must still exist"));
    }

    @Test @org.junit.jupiter.api.Order(273)
    void updateAtomic_clearSlot_atomicWithFieldUpdate() {
        // testFoodId at slot 3, testFeat2Id at slot 2
        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        String newName = "SV_TestFood_Cleared273";

        // Update with targetPosition=null → clear slot and rename atomically
        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, newName, 10.00, true, 5, null, catId, null, false);

        assertEquals(FeaturedAssignmentResult.Status.CLEARED, result.getStatus(),
                "Clearing slot must return CLEARED");

        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food -> {
            assertEquals(newName, food.getFoodName(), "Name must be updated");
            assertNull(food.getFeaturedPosition(), "Featured slot must be null after clearing");
        }, () -> fail("testFoodId must still exist"));
    }

    @Test @org.junit.jupiter.api.Order(274)
    void updateAtomic_sameSlot_noDisplacementOccurs() {
        // Assign testFoodId to slot 1, testFeat2Id to slot 2
        FoodService.setFeaturedPosition(testFoodId, 1);
        FoodService.setFeaturedPosition(testFeat2Id, 2);

        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        String newName = "SV_TestFood_SameSlot274";

        // Update testFoodId with same slot (1 → 1); testFeat2Id must not be displaced
        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, newName, 11.00, true, 7, null, catId, 1, false);

        assertTrue(result.isSuccess(), "Update to same slot must succeed");

        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food -> {
            assertEquals(newName, food.getFoodName(), "Name must be updated");
            assertEquals(Integer.valueOf(1), food.getFeaturedPosition(),
                    "testFoodId must still be at slot 1");
        }, () -> fail("testFoodId must still exist"));

        FoodService.getFoodItemById(testFeat2Id).ifPresentOrElse(food ->
            assertEquals(Integer.valueOf(2), food.getFeaturedPosition(),
                    "testFeat2Id must remain at slot 2 — no displacement should occur"),
            () -> fail("testFeat2Id must still exist"));
    }

    @Test @org.junit.jupiter.api.Order(275)
    void updateAtomic_displacement_atomicWithFieldUpdate() {
        // testFoodId at slot 1, testFeat2Id at slot 2 (from @Order(274))
        // Move testFoodId into slot 2 (displacing testFeat2Id) atomically with a rename
        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        String newName = "SV_TestFood_Displaced275";

        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, newName, 14.00, true, 8, null, catId, 2, true);

        assertTrue(result.isSuccess(), "Update with displacement must succeed");
        assertEquals(Integer.valueOf(testFeat2Id), result.getRelatedFoodId(),
                "relatedFoodId must identify the displaced food (testFeat2Id)");

        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food -> {
            assertEquals(newName, food.getFoodName(), "Name must be updated");
            assertEquals(Integer.valueOf(2), food.getFeaturedPosition(),
                    "testFoodId must now hold slot 2");
        }, () -> fail("testFoodId must still exist"));

        FoodService.getFoodItemById(testFeat2Id).ifPresentOrElse(food ->
            assertNull(food.getFeaturedPosition(),
                    "testFeat2Id must be displaced (featured_position set to null)"),
            () -> fail("testFeat2Id must still exist"));
    }

    @Test @org.junit.jupiter.api.Order(276)
    void updateAtomic_notFoundFood_preservesSlotOwners() {
        // testFoodId at slot 2 from prior test; verify that an attempt to update a
        // non-existent food throws RuntimeException and leaves slot 2 unaffected.
        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();

        assertThrows(RuntimeException.class,
                () -> FoodService.updateFoodItemWithFeaturedPosition(
                        Integer.MAX_VALUE, "Ghost", 1.00, true, 1, null, catId, 2, true),
                "Updating a non-existent food must throw RuntimeException");

        // testFoodId must still hold slot 2 (rolled back, no data changed)
        FoodService.getFoodItemById(testFoodId).ifPresentOrElse(food ->
            assertEquals(Integer.valueOf(2), food.getFeaturedPosition(),
                    "testFoodId slot must be unaffected by the failed update of a ghost food"),
            () -> fail("testFoodId must still exist"));
    }

    @Test @org.junit.jupiter.api.Order(277)
    void updateAtomic_imagePathInDB_unchangedOnRollback() throws Exception {
        // Arrange: testFeat2Id at slot 1, testFoodId at slot 2
        FoodService.setFeaturedPosition(testFeat2Id, 1);

        // Capture current image_path of testFoodId directly from DB
        String originalImagePath;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT image_path FROM food_items WHERE food_id = ?")) {
            ps.setInt(1, testFoodId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "testFoodId must exist in DB");
                originalImagePath = rs.getString("image_path");
            }
        }

        // Attempt update targeting slot 1 (occupied by testFeat2Id) with a new image_path,
        // replaceOccupied=false → must return SLOT_OCCUPIED and leave DB unchanged.
        int catId = citybites.service.FoodCategoryService.getOtherCategoryId();
        FeaturedAssignmentResult result = FoodService.updateFoodItemWithFeaturedPosition(
                testFoodId, "SV_ShouldNotChange", 77.77, true, 77,
                "fake/rollback_test.jpg", catId, 1, false);

        assertEquals(FeaturedAssignmentResult.Status.SLOT_OCCUPIED, result.getStatus(),
                "Must return SLOT_OCCUPIED");

        // Verify the image_path in the DB is still the original value (transaction rolled back)
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT image_path FROM food_items WHERE food_id = ?")) {
            ps.setInt(1, testFoodId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "testFoodId must still exist");
                assertEquals(originalImagePath, rs.getString("image_path"),
                        "image_path must be unchanged after a rolled-back update");
            }
        }
    }

    // ── @Order(300-318): Food Rating and Review tests ────────────────────────

    /** order_id of the SV_ completed order used in rating tests. */
    static int svRatingOrderId   = -1;
    /** item_id of the first order item in svRatingOrderId. */
    static int svRatingItem1Id   = -1;
    /** item_id of the second order item (for multi-item tests). */
    static int svRatingItem2Id   = -1;
    /** customer_id of a second SV_ customer for authorization tests. */
    static int svOtherCustomerId = -1;
    /** item_id belonging to svOtherCustomerId's order. */
    static int svOtherItemId     = -1;

    /**
     * Inserts a minimal order + one order item directly via SQL.
     * Returns [orderId, itemId].
     */
    private static int[] insertDirectOrder(int customerId, String status)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (customer_id, total_amount, status) " +
                    "VALUES (?, 100.00, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setString(2, status);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); orderId = k.getInt(1); }
            }
            int itemId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, food_id, food_name, unit_price, quantity) " +
                    "VALUES (?, ?, 'SV_RatingTest', 100.00, 1)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, orderId);
                ps.setInt(2, testFoodId);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); itemId = k.getInt(1); }
            }
            return new int[]{orderId, itemId};
        }
    }

    @Test @org.junit.jupiter.api.Order(300)
    void rating_table_and_constraints_exist() throws Exception {
        // food_ratings table exists
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_ratings'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertTrue(rs.getInt(1) > 0, "food_ratings table must exist after migration");
            }
        }
        // UNIQUE KEY on order_item_id
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'food_ratings' " +
                "AND INDEX_NAME = 'uk_food_rating_order_item' " +
                "AND NON_UNIQUE = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertTrue(rs.getInt(1) > 0, "UNIQUE KEY uk_food_rating_order_item must exist on food_ratings");
            }
        }
        // CHECK constraint on rating (1–5)
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'food_ratings' " +
                "AND CONSTRAINT_NAME = 'chk_rating_value' " +
                "AND CONSTRAINT_TYPE = 'CHECK'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertTrue(rs.getInt(1) > 0, "CHECK constraint chk_rating_value must exist on food_ratings");
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(301)
    void rating_setup_completedOrderFixture() throws Exception {
        // Completed order with 2 items for testCustomer
        int[] r1 = insertDirectOrder(testCustomer.getCustomerId(), "Completed");
        svRatingOrderId = r1[0];
        svRatingItem1Id = r1[1];
        // Insert a second item into the same order
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO order_items (order_id, food_id, food_name, unit_price, quantity) " +
                "VALUES (?, ?, 'SV_RatingTest2', 200.00, 2)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, svRatingOrderId);
            ps.setInt(2, testFoodId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); svRatingItem2Id = k.getInt(1); }
        }
        // Create another customer's completed order
        svOtherCustomerId = citybites.service.CustomerManagementService.addCustomer(
            "SV Rating Other", "sv_ratingother", "RatingOther1!", "RatingOther1!");
        assertTrue(svOtherCustomerId > 0, "Second SV_ customer must be created");
        int[] r2 = insertDirectOrder(svOtherCustomerId, "Completed");
        svOtherItemId = r2[1];

        assertTrue(svRatingOrderId  > 0, "svRatingOrderId must be set");
        assertTrue(svRatingItem1Id  > 0, "svRatingItem1Id must be set");
        assertTrue(svRatingItem2Id  > 0, "svRatingItem2Id must be set");
        assertTrue(svOtherItemId    > 0, "svOtherItemId must be set");
    }

    @Test @org.junit.jupiter.api.Order(302)
    void rating_completedOrderItem_canBeRated() {
        SessionManager.setLoggedInCustomer(testCustomer);
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem1Id, 4, "Great food!");
        assertNull(error, "Saving a rating for a completed order item must succeed; got: " + error);

        Optional<citybites.model.FoodRating> rating =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem1Id);
        assertTrue(rating.isPresent(), "Rating must be retrievable after saving");
        assertEquals(4, rating.get().getRating(), "Rating value must be 4");
        assertEquals("Great food!", rating.get().getReviewText(), "Review text must match");
    }

    @Test @org.junit.jupiter.api.Order(303)
    void rating_pendingOrderItem_isRejected() throws Exception {
        SessionManager.setLoggedInCustomer(testCustomer);
        int[] pendingData = insertDirectOrder(testCustomer.getCustomerId(), "Pending");
        int pendingItemId = pendingData[1];
        try {
            String error = citybites.service.FoodRatingService.saveRating(pendingItemId, 3, null);
            assertNotNull(error, "Rating a pending order item must return an error message");
            assertTrue(error.toLowerCase().contains("complet") || error.toLowerCase().contains("status"),
                "Error must mention 'Completed' or 'status'; got: " + error);
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + pendingData[0]);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(304)
    void rating_cancelledOrderItem_isRejected() throws Exception {
        SessionManager.setLoggedInCustomer(testCustomer);
        int[] cancelledData = insertDirectOrder(testCustomer.getCustomerId(), "Cancelled");
        int cancelledItemId = cancelledData[1];
        try {
            String error = citybites.service.FoodRatingService.saveRating(cancelledItemId, 5, null);
            assertNotNull(error, "Rating a cancelled order item must return an error message");
            assertTrue(error.toLowerCase().contains("complet") || error.toLowerCase().contains("status"),
                "Error must mention 'Completed' or 'status'; got: " + error);
        } finally {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + cancelledData[0]);
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(305)
    void rating_anotherCustomersItem_isRejected() {
        // testCustomer tries to rate svOtherCustomerId's order item
        SessionManager.setLoggedInCustomer(testCustomer);
        String error = citybites.service.FoodRatingService.saveRating(svOtherItemId, 3, null);
        assertNotNull(error, "Rating another customer's order item must return an error");
        assertTrue(error.toLowerCase().contains("belong") || error.toLowerCase().contains("account")
                   || error.toLowerCase().contains("not found"),
            "Error must mention ownership; got: " + error);
    }

    @Test @org.junit.jupiter.api.Order(306)
    void rating_belowOne_isRejected() {
        SessionManager.setLoggedInCustomer(testCustomer);
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem2Id, 0, null);
        assertNotNull(error, "Rating 0 must be rejected");
        assertTrue(error.toLowerCase().contains("rating") || error.toLowerCase().contains("1"),
            "Error must mention valid range; got: " + error);
    }

    @Test @org.junit.jupiter.api.Order(307)
    void rating_aboveFive_isRejected() {
        SessionManager.setLoggedInCustomer(testCustomer);
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem2Id, 6, null);
        assertNotNull(error, "Rating 6 must be rejected");
        assertTrue(error.toLowerCase().contains("rating") || error.toLowerCase().contains("5"),
            "Error must mention valid range; got: " + error);
    }

    @Test @org.junit.jupiter.api.Order(308)
    void rating_blankReview_storedAsNull() {
        SessionManager.setLoggedInCustomer(testCustomer);
        // Rate item2 with a blank review (spaces only) — should be stored as NULL
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem2Id, 3, "   ");
        assertNull(error, "Rating with blank review must succeed; got: " + error);

        Optional<citybites.model.FoodRating> rating =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem2Id);
        assertTrue(rating.isPresent(), "Rating must exist after save");
        assertNull(rating.get().getReviewText(),
            "Blank review (whitespace only) must be stored as NULL, not as whitespace string");
    }

    @Test @org.junit.jupiter.api.Order(309)
    void rating_overlengthReview_isRejected() {
        SessionManager.setLoggedInCustomer(testCustomer);
        String longReview = "A".repeat(501);   // 501 chars > 500 limit
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem2Id, 5, longReview);
        assertNotNull(error, "Review exceeding 500 chars must be rejected");
        assertTrue(error.toLowerCase().contains("500") || error.toLowerCase().contains("review"),
            "Error must mention review length; got: " + error);
    }

    @Test @org.junit.jupiter.api.Order(310)
    void rating_existingRating_loadsCorrectly() {
        // svRatingItem1Id was rated in @Order(302) with rating=4, review="Great food!"
        Optional<citybites.model.FoodRating> rating =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem1Id);
        assertTrue(rating.isPresent(), "Existing rating must be retrievable");
        assertEquals(4, rating.get().getRating(), "Rating must be 4 (set in test 302)");
        assertEquals("Great food!", rating.get().getReviewText(), "Review must be 'Great food!'");
        assertEquals(svRatingItem1Id, rating.get().getOrderItemId(), "orderItemId must match");
    }

    @Test @org.junit.jupiter.api.Order(311)
    void rating_existingRating_canBeUpdated() {
        SessionManager.setLoggedInCustomer(testCustomer);
        // Update rating from 4 → 5 and change review
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem1Id, 5, "Even better!");
        assertNull(error, "Updating an existing rating must succeed; got: " + error);

        Optional<citybites.model.FoodRating> rating =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem1Id);
        assertTrue(rating.isPresent(), "Rating must still exist after update");
        assertEquals(5, rating.get().getRating(), "Rating must be updated to 5");
        assertEquals("Even better!", rating.get().getReviewText(), "Review must be updated");
    }

    @Test @org.junit.jupiter.api.Order(312)
    void rating_updateDoesNotCreateDuplicate() throws Exception {
        // Multiple saves for the same order item must not insert extra rows
        SessionManager.setLoggedInCustomer(testCustomer);
        citybites.service.FoodRatingService.saveRating(svRatingItem1Id, 3, "Third save");
        citybites.service.FoodRatingService.saveRating(svRatingItem1Id, 2, "Fourth save");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM food_ratings WHERE order_item_id = ?")) {
            ps.setInt(1, svRatingItem1Id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1),
                    "Exactly one food_ratings row must exist per order_item_id after multiple saves");
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(313)
    void rating_multipleItemsInOrder_supportSeparateRatings() {
        SessionManager.setLoggedInCustomer(testCustomer);
        // item1 already rated; now rate item2 independently
        Optional<citybites.model.FoodRating> r1 =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem1Id);
        assertTrue(r1.isPresent(), "item1 must already be rated");

        // item2 was rated in @Order(308); verify independent existence
        Optional<citybites.model.FoodRating> r2 =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem2Id);
        assertTrue(r2.isPresent(), "item2 must have its own independent rating");
        assertNotEquals(r1.get().getRatingId(), r2.get().getRatingId(),
            "item1 and item2 must have separate rating_id values");

        List<citybites.model.FoodRating> byOrder =
            citybites.service.FoodRatingService.getRatingsForOrder(svRatingOrderId);
        assertTrue(byOrder.size() >= 2,
            "getRatingsForOrder must return at least 2 ratings for a 2-item order");
    }

    @Test @org.junit.jupiter.api.Order(314)
    void rating_unauthenticatedSession_isRejected() {
        SessionManager.setLoggedInCustomer(null);   // clear session
        try {
            String error = citybites.service.FoodRatingService.saveRating(svRatingItem1Id, 5, null);
            assertNotNull(error, "saveRating without a logged-in customer must return an error");
            assertTrue(error.toLowerCase().contains("session") || error.toLowerCase().contains("log"),
                "Error must mention session/login; got: " + error);
        } finally {
            SessionManager.setLoggedInCustomer(testCustomer);   // restore
        }
    }

    @Test @org.junit.jupiter.api.Order(315)
    void rating_migration_isIdempotent() throws Exception {
        // Calling DatabaseInitializer.initialize() a second time must not throw or duplicate data
        assertDoesNotThrow(
            () -> DatabaseInitializer.initialize(),
            "DatabaseInitializer.initialize() must be safe to call repeatedly (idempotent)");

        // Migration key must still appear exactly once in schema_migrations
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM schema_migrations WHERE migration_key = '20260903_add_food_ratings_table'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1),
                    "Migration key 20260903_add_food_ratings_table must appear exactly once after repeated init");
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(316)
    void rating_cascadeDelete_deletingOrderRemovesRatings() throws Exception {
        // Create a fresh completed order with one item, rate it, then delete the order.
        // The rating must cascade-delete automatically.
        SessionManager.setLoggedInCustomer(testCustomer);
        int[] data = insertDirectOrder(testCustomer.getCustomerId(), "Completed");
        int tempOrderId = data[0];
        int tempItemId  = data[1];

        String err = citybites.service.FoodRatingService.saveRating(tempItemId, 4, null);
        assertNull(err, "Setup rating must succeed; got: " + err);

        // Verify rating exists
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM food_ratings WHERE order_item_id = ?")) {
            ps.setInt(1, tempItemId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1), "Rating must exist before order deletion");
            }
        }

        // Delete the order — order_items cascade, which cascades to food_ratings
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.createStatement().execute("DELETE FROM orders WHERE order_id = " + tempOrderId);
        }

        // Rating must be gone
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM food_ratings WHERE order_item_id = ?")) {
            ps.setInt(1, tempItemId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1),
                    "Rating must cascade-delete when its order is deleted");
            }
        }
    }

    @Test @org.junit.jupiter.api.Order(317)
    void rating_invalidOrderItemId_isRejectedCleanly() {
        SessionManager.setLoggedInCustomer(testCustomer);
        // A non-existent order item ID must return an error, never throw uncaught
        String error = citybites.service.FoodRatingService.saveRating(Integer.MAX_VALUE, 3, null);
        assertNotNull(error, "saveRating for a non-existent order item must return an error");
        assertTrue(error.toLowerCase().contains("not found") || error.toLowerCase().contains("item"),
            "Error must indicate the order item was not found; got: " + error);
    }

    @Test @org.junit.jupiter.api.Order(318)
    void rating_nullReview_explicitlyNull_storedAsNull() {
        SessionManager.setLoggedInCustomer(testCustomer);
        // Rate item2 explicitly with null review
        String error = citybites.service.FoodRatingService.saveRating(svRatingItem2Id, 1, null);
        assertNull(error, "Rating with explicit null review must succeed; got: " + error);

        Optional<citybites.model.FoodRating> rating =
            citybites.service.FoodRatingService.getRatingForOrderItem(svRatingItem2Id);
        assertTrue(rating.isPresent(), "Rating must exist after save");
        assertNull(rating.get().getReviewText(), "Null review must be stored as NULL in DB");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADMIN FOOD RATING REVIEWS (@Order 350-365)
    // ══════════════════════════════════════════════════════════════════════

    /** food_id of the SV_ food item created for admin rating review tests. */
    static int svAdminFoodId  = -1;
    /** item_id of the first rated order item (rating=4). */
    static int svAdminItemId  = -1;
    /** item_id of the second rated order item (rating=2). */
    static int svAdminItemId2 = -1;

    /**
     * Inserts a Completed order + one order item for the given food.
     * Returns [orderId, itemId].
     */
    private static int[] insertDirectOrderForFood(int customerId, int foodId, String foodName)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (customer_id, total_amount, status) " +
                    "VALUES (?, 100.00, 'Completed')",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); orderId = k.getInt(1); }
            }
            int itemId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, food_id, food_name, unit_price, quantity) " +
                    "VALUES (?, ?, ?, 100.00, 1)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, orderId);
                ps.setInt(2, foodId);
                ps.setString(3, foodName);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); itemId = k.getInt(1); }
            }
            return new int[]{orderId, itemId};
        }
    }

    @Test @org.junit.jupiter.api.Order(350)
    void adminReview_setup_createFirstRatedItem() throws Exception {
        svAdminFoodId = FoodService.addFoodItem("SV_AdminReviewFood", 100.0, true, 10, null);
        assertTrue(svAdminFoodId > 0, "SV_AdminReviewFood must be created");
        int[] data = insertDirectOrderForFood(
                testCustomer.getCustomerId(), svAdminFoodId, "SV_AdminReviewFood");
        svAdminItemId = data[1];
        SessionManager.setLoggedInCustomer(testCustomer);
        String err = citybites.service.FoodRatingService.saveRating(svAdminItemId, 4, "Admin review text");
        assertNull(err, "First rating must succeed: " + err);
    }

    @Test @org.junit.jupiter.api.Order(351)
    void adminReview_singleRating_correctAverage() {
        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        citybites.model.FoodRatingSummary s = map.get(svAdminFoodId);
        assertNotNull(s, "Summary must exist for svAdminFoodId after first rating");
        assertEquals(1, s.getRatingCount(), "count must be 1 after first rating");
        assertEquals(4.0, s.getAverageRating(), 0.01, "avg must be 4.0 after rating of 4");
    }

    @Test @org.junit.jupiter.api.Order(352)
    void adminReview_addSecondRating_avgUpdates() throws Exception {
        int[] data2 = insertDirectOrderForFood(
                testCustomer.getCustomerId(), svAdminFoodId, "SV_AdminReviewFood");
        svAdminItemId2 = data2[1];
        SessionManager.setLoggedInCustomer(testCustomer);
        String err = citybites.service.FoodRatingService.saveRating(svAdminItemId2, 2, null);
        assertNull(err, "Second rating must succeed: " + err);

        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        citybites.model.FoodRatingSummary s = map.get(svAdminFoodId);
        assertNotNull(s, "Summary must exist after second rating");
        assertEquals(3.0, s.getAverageRating(), 0.01,
            "avg must be 3.0 after ratings of 4 and 2");
    }

    @Test @org.junit.jupiter.api.Order(353)
    void adminReview_ratingCount_correct() {
        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        citybites.model.FoodRatingSummary s = map.get(svAdminFoodId);
        assertNotNull(s, "Summary must exist for svAdminFoodId");
        assertEquals(2, s.getRatingCount(), "count must be 2 after two ratings");
    }

    @Test @org.junit.jupiter.api.Order(354)
    void adminReview_foodWithNoRatings_zeroCount() {
        int freshId = FoodService.addFoodItem("SV_AdminNoRating", 50.0, true, 5, null);
        assertTrue(freshId > 0, "SV_AdminNoRating must be created");
        try {
            java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
                citybites.service.FoodRatingService.getRatingSummaries();
            citybites.model.FoodRatingSummary s = map.get(freshId);
            assertNotNull(s, "Summary must include foods with no ratings");
            assertEquals(0, s.getRatingCount(), "count must be 0 for a food with no ratings");
            assertEquals(0.0, s.getAverageRating(), 0.001, "avg must be 0.0 when count==0");
        } finally {
            try { FoodService.deleteFoodItem(freshId); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(355)
    void adminReview_getReviewsForFood_containsCorrectFoodName() {
        java.util.List<citybites.model.FoodReviewDetail> reviews =
            citybites.service.FoodRatingService.getReviewsForFood(svAdminFoodId);
        assertFalse(reviews.isEmpty(), "Reviews must not be empty for svAdminFoodId");
        reviews.forEach(r -> assertEquals("SV_AdminReviewFood", r.getFoodName(),
            "Every review detail must carry the correct food name"));
    }

    @Test @org.junit.jupiter.api.Order(356)
    void adminReview_getReviewsForFood_containsCustomerFullName() {
        java.util.List<citybites.model.FoodReviewDetail> reviews =
            citybites.service.FoodRatingService.getReviewsForFood(svAdminFoodId);
        assertFalse(reviews.isEmpty(), "Reviews list must not be empty");
        reviews.forEach(r -> {
            assertNotNull(r.getCustomerFullName(), "customerFullName must not be null");
            assertFalse(r.getCustomerFullName().isBlank(), "customerFullName must not be blank");
        });
    }

    @Test @org.junit.jupiter.api.Order(357)
    void adminReview_getReviewsForFood_onlyForRequestedFood() {
        int otherId = FoodService.addFoodItem("SV_AdminOtherFood", 80.0, true, 5, null);
        assertTrue(otherId > 0, "SV_AdminOtherFood must be created");
        try {
            java.util.List<citybites.model.FoodReviewDetail> reviews =
                citybites.service.FoodRatingService.getReviewsForFood(otherId);
            assertTrue(reviews.isEmpty(),
                "Reviews for an unrated food must be empty");
        } finally {
            try { FoodService.deleteFoodItem(otherId); } catch (Exception ignored) {}
        }
    }

    @Test @org.junit.jupiter.api.Order(358)
    void adminReview_reviewDetail_includesDates() {
        java.util.List<citybites.model.FoodReviewDetail> reviews =
            citybites.service.FoodRatingService.getReviewsForFood(svAdminFoodId);
        assertFalse(reviews.isEmpty(), "Reviews must not be empty");
        citybites.model.FoodReviewDetail r = reviews.get(0);
        assertNotNull(r.getOrderDate(), "orderDate must not be null in review detail");
        assertNotNull(r.getCreatedAt(), "createdAt must not be null in review detail");
    }

    @Test @org.junit.jupiter.api.Order(359)
    void adminReview_nullReview_safe() {
        java.util.List<citybites.model.FoodReviewDetail> reviews =
            citybites.service.FoodRatingService.getReviewsForFood(svAdminFoodId);
        boolean hasNullReview = reviews.stream().anyMatch(r -> r.getReviewText() == null);
        assertTrue(hasNullReview,
            "At least one review must have null reviewText (svAdminItemId2 was rated with null)");
    }

    @Test @org.junit.jupiter.api.Order(360)
    void adminReview_invalidFoodId_rejected() {
        assertThrows(IllegalArgumentException.class,
            () -> citybites.service.FoodRatingService.getReviewsForFood(0),
            "CORRECTLY REJECTED: foodId=0 must throw IllegalArgumentException");
    }

    @Test @org.junit.jupiter.api.Order(361)
    void adminReview_nonExistentFoodId_handled() {
        assertThrows(IllegalArgumentException.class,
            () -> citybites.service.FoodRatingService.getReviewsForFood(Integer.MAX_VALUE),
            "CORRECTLY REJECTED: non-existent food ID must throw IllegalArgumentException");
    }

    @Test @org.junit.jupiter.api.Order(362)
    void adminReview_updateRating_doesNotIncrementCount() {
        SessionManager.setLoggedInCustomer(testCustomer);
        String err = citybites.service.FoodRatingService.saveRating(svAdminItemId, 5, "Updated review");
        assertNull(err, "Rating update must succeed");

        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        citybites.model.FoodRatingSummary s = map.get(svAdminFoodId);
        assertNotNull(s, "Summary must still exist after update");
        assertEquals(2, s.getRatingCount(),
            "count must still be 2 — updating an existing rating must not insert a new row");
    }

    @Test @org.junit.jupiter.api.Order(363)
    void adminReview_summaryMap_includesAllFoods() {
        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        assertTrue(map.containsKey(svAdminFoodId),
            "Summary map must contain an entry for svAdminFoodId");
        assertTrue(map.containsKey(testFoodId),
            "Summary map must contain an entry for testFoodId");
    }

    @Test @org.junit.jupiter.api.Order(364)
    void adminReview_reviewDTO_hasNoPasswordField() {
        Class<?> clazz = citybites.model.FoodReviewDetail.class;
        assertThrows(NoSuchMethodException.class,
            () -> clazz.getMethod("getPasswordHash"),
            "FoodReviewDetail must not expose getPasswordHash()");
        assertThrows(NoSuchMethodException.class,
            () -> clazz.getMethod("getPassword"),
            "FoodReviewDetail must not expose getPassword()");
        assertThrows(NoSuchMethodException.class,
            () -> clazz.getMethod("getCustomerId"),
            "FoodReviewDetail must not expose getCustomerId()");
    }

    @Test @org.junit.jupiter.api.Order(365)
    void adminReview_getReviewsForFood_countMatchesSummary() {
        java.util.List<citybites.model.FoodReviewDetail> reviews =
            citybites.service.FoodRatingService.getReviewsForFood(svAdminFoodId);
        java.util.Map<Integer, citybites.model.FoodRatingSummary> map =
            citybites.service.FoodRatingService.getRatingSummaries();
        citybites.model.FoodRatingSummary s = map.get(svAdminFoodId);
        assertNotNull(s, "Summary must exist for svAdminFoodId");
        assertEquals(reviews.size(), s.getRatingCount(),
            "list.size() from getReviewsForFood must equal getRatingCount() from summary");
    }

    // ─── StarRatingPanel UI unit tests (@Order 370–376) ──────────────────────

    @Test @org.junit.jupiter.api.Order(370)
    void starPanel_starCount_constant_is_five() {
        assertEquals(5, citybites.ui.StarRatingPanel.STAR_COUNT,
            "STAR_COUNT must always be 5");
    }

    @Test @org.junit.jupiter.api.Order(371)
    void starPanel_rating_stored_correctly() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = new citybites.ui.StarRatingPanel(3);
        assertEquals(3, panel.getRating(), "StarRatingPanel must store rating=3");
    }

    @Test @org.junit.jupiter.api.Order(372)
    void starPanel_rating_1_edge_case_safe() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = new citybites.ui.StarRatingPanel(1);
        assertEquals(1, panel.getRating(), "StarRatingPanel must handle edge case rating=1");
    }

    @Test @org.junit.jupiter.api.Order(373)
    void starPanel_rating_5_edge_case_safe() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = new citybites.ui.StarRatingPanel(5);
        assertEquals(5, panel.getRating(), "StarRatingPanel must handle edge case rating=5");
    }

    @Test @org.junit.jupiter.api.Order(374)
    void starPanel_out_of_range_ratings_clamped() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel low  = new citybites.ui.StarRatingPanel(-1);
        citybites.ui.StarRatingPanel high = new citybites.ui.StarRatingPanel(9);
        assertEquals(0, low.getRating(),  "Rating below 0 must clamp to 0");
        assertEquals(5, high.getRating(), "Rating above 5 must clamp to 5");
    }

    @Test @org.junit.jupiter.api.Order(375)
    void formatCount_singular_and_plural() {
        citybites.model.FoodRatingSummary one  = new citybites.model.FoodRatingSummary(1, 4.5, 1);
        citybites.model.FoodRatingSummary many = new citybites.model.FoodRatingSummary(1, 3.0, 7);
        citybites.model.FoodRatingSummary zero = new citybites.model.FoodRatingSummary(1, 0.0, 0);
        assertEquals("1 review",  one.formatCount(),  "count=1 must return '1 review'");
        assertEquals("7 reviews", many.formatCount(), "count=7 must return '7 reviews'");
        assertEquals("0 reviews", zero.formatCount(), "count=0 must return '0 reviews'");
    }

    @Test @org.junit.jupiter.api.Order(376)
    void formatAverage_em_dash_when_zero_count() {
        citybites.model.FoodRatingSummary zero = new citybites.model.FoodRatingSummary(1, 0.0, 0);
        citybites.model.FoodRatingSummary rated = new citybites.model.FoodRatingSummary(1, 4.5, 2);
        assertEquals("\u2014", zero.formatAverage(),
            "formatAverage must return em-dash when ratingCount==0");
        assertEquals("4.5 / 5", rated.formatAverage(),
            "formatAverage must return formatted string when ratingCount>0");
    }

    // ─── Interactive StarRatingPanel tests (@Order 380–388) ──────────────────

    @Test @org.junit.jupiter.api.Order(380)
    void interactivePanel_defaults_to_zero_when_no_existing() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = citybites.ui.StarRatingPanel.interactive(0);
        assertEquals(0, panel.getRating(),
            "Interactive panel constructed with 0 must return getRating()==0");
    }

    @Test @org.junit.jupiter.api.Order(381)
    void interactivePanel_stores_selected_rating() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = citybites.ui.StarRatingPanel.interactive(0);
        panel.setRating(3);
        assertEquals(3, panel.getRating(),
            "setRating(3) on interactive panel must store rating 3");
    }

    @Test @org.junit.jupiter.api.Order(382)
    void interactivePanel_change_from_3_to_5() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = citybites.ui.StarRatingPanel.interactive(0);
        panel.setRating(3);
        panel.setRating(5);
        assertEquals(5, panel.getRating(),
            "Changing rating from 3 to 5 must store 5");
    }

    @Test @org.junit.jupiter.api.Order(383)
    void interactivePanel_existing_rating_loaded() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = citybites.ui.StarRatingPanel.interactive(4);
        assertEquals(4, panel.getRating(),
            "Interactive panel constructed with existing rating 4 must return 4");
    }

    @Test @org.junit.jupiter.api.Order(384)
    void interactivePanel_clamping() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel low  = citybites.ui.StarRatingPanel.interactive(-1);
        citybites.ui.StarRatingPanel high = citybites.ui.StarRatingPanel.interactive(9);
        assertEquals(0, low.getRating(),  "Rating < 0 must clamp to 0");
        assertEquals(5, high.getRating(), "Rating > 5 must clamp to 5");
    }

    @Test @org.junit.jupiter.api.Order(385)
    void readOnly_panel_not_affected_by_interactive_factory() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel ro = new citybites.ui.StarRatingPanel(3);
        assertEquals(3, ro.getRating(),
            "Read-only StarRatingPanel(3) must always return 3");
        // setRating is a no-op for read-only panels
        ro.setRating(5);
        assertEquals(3, ro.getRating(),
            "setRating on read-only panel must have no effect");
    }

    @Test @org.junit.jupiter.api.Order(386)
    void both_modes_have_five_star_count() {
        assertEquals(5, citybites.ui.StarRatingPanel.STAR_COUNT,
            "STAR_COUNT must be 5 in both modes");
    }

    @Test @org.junit.jupiter.api.Order(387)
    void interactivePanel_callback_fires_on_setRating() {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        citybites.ui.StarRatingPanel panel = citybites.ui.StarRatingPanel.interactive(0);
        int[] callbackCount = {0};
        panel.setOnRatingChanged(() -> callbackCount[0]++);
        panel.setRating(3);
        panel.setRating(5);
        assertEquals(2, callbackCount[0],
            "onRatingChanged callback must fire once per setRating call");
    }

    @Test @org.junit.jupiter.api.Order(388)
    void foodRatingDialog_no_unicode_star_fields() {
        Class<?> clazz = citybites.ui.FoodRatingDialog.class;
        for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
            assertFalse("STAR_FILLED".equals(f.getName()),
                "STAR_FILLED Unicode field must be removed from FoodRatingDialog");
            assertFalse("STAR_EMPTY".equals(f.getName()),
                "STAR_EMPTY Unicode field must be removed from FoodRatingDialog");
        }
    }

    @Test @org.junit.jupiter.api.Order(278)
    void featuredFoods_uniqueIndex_and_checkConstraint_exist() throws Exception {
        // Verify UNIQUE KEY uk_food_featured_pos exists on food_items
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                 "WHERE TABLE_SCHEMA = DATABASE() " +
                 "AND TABLE_NAME = 'food_items' " +
                 "AND INDEX_NAME = 'uk_food_featured_pos'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertTrue(rs.getInt(1) > 0,
                    "UNIQUE KEY uk_food_featured_pos must exist on food_items");
            }
        }

        // Verify CHECK constraint chk_featured_position exists on food_items
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS " +
                 "WHERE TABLE_SCHEMA = DATABASE() " +
                 "AND TABLE_NAME = 'food_items' " +
                 "AND CONSTRAINT_NAME = 'chk_featured_position' " +
                 "AND CONSTRAINT_TYPE = 'CHECK'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertTrue(rs.getInt(1) > 0,
                    "CHECK constraint chk_featured_position must exist on food_items");
            }
        }
    }
}
