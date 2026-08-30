package citybites.dao;

import citybites.model.FoodItem;
import citybites.model.Order;
import citybites.model.OrderItem;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data-access contract for the orders / order_items tables.
 */
public interface OrderDAO {

    // -- Transaction-aware (caller provides the connection and manages tx) --

    /** Inserts the order header row; returns the generated order_id. */
    int insertOrderHeader(Connection conn, Order order) throws SQLException;

    /** Inserts all order-item rows for the given order. */
    void insertOrderItems(Connection conn, int orderId, List<OrderItem> items) throws SQLException;

    /**
     * Locks the food_item row for update and returns its current state.
     * Must be called within an active transaction.
     */
    FoodItem lockFoodItemForUpdate(Connection conn, int foodId) throws SQLException;

    /**
     * Deducts quantity from stock_quantity using a safe conditional UPDATE.
     * Returns false if the row was not updated (insufficient stock guard).
     */
    boolean deductStock(Connection conn, int foodId, int quantity) throws SQLException;

    /** Sets available = false when stock reaches zero. */
    void updateAvailabilityIfEmpty(Connection conn, int foodId) throws SQLException;

    // -- Standard reads (manage their own connections) --

    List<Order> findByCustomerId(int customerId);

    List<Order> findAll();

    List<OrderItem> findItemsByOrderId(int orderId);

    String getStatusById(int orderId);

    int countPending();

    // -- Status management --

    boolean updateStatus(int orderId, String newStatus);

    /**
     * Restores stock for every item in the given order.
     * Runs inside its own transaction.
     * Also re-enables availability when stock becomes positive.
     * Safe to call only once -- caller must verify the order is not already Cancelled.
     */
    boolean restoreStockAndAvailability(int orderId);
}
