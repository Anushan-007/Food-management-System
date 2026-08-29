package citybites.dao;

import citybites.model.Order;
import citybites.model.OrderItem;
import java.util.List;

/**
 * Data-access contract for the orders / order_items tables.
 */
public interface OrderDAO {

    /**
     * Inserts a new order and its items inside a single transaction.
     * Returns the auto-generated order ID, or -1 on failure.
     */
    int insertOrder(Order order);

    /** Returns all orders for a given customer, ordered by date desc. */
    List<Order> findByCustomerId(int customerId);

    /** Returns every order in the system, ordered by date desc. */
    List<Order> findAll();

    /** Updates the status column of a single order. Returns true on success. */
    boolean updateStatus(int orderId, String status);

    /** Returns the order items for a specific order. */
    List<OrderItem> findItemsByOrderId(int orderId);
}
