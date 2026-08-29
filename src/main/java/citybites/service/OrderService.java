package citybites.service;

import citybites.config.DatabaseConnection;
import citybites.dao.OrderDAO;
import citybites.dao.impl.OrderDAOImpl;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.FoodItem;
import citybites.model.Order;
import citybites.model.OrderItem;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles order placement (with atomic stock deduction) and status management.
 */
public class OrderService {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());
    private static final OrderDAO orderDAO = new OrderDAOImpl();

    private OrderService() {}

    /**
     * Places an order atomically:
     * 1. Acquires row-level locks on food_items (FOR UPDATE)
     * 2. Validates availability and stock for each cart item
     * 3. Inserts the order header
     * 4. Inserts all order items
     * 5. Deducts stock with a safe conditional UPDATE
     * 6. Marks items unavailable when stock reaches zero
     * 7. Commits \u2014 or rolls back completely on any failure
     *
     * The cart is NOT cleared here; callers clear it only after this returns successfully.
     */
    public static Order placeOrder(Customer customer, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 1 & 2: Lock rows and validate stock
                List<OrderItem> orderItems = new ArrayList<>();
                double total = 0.0;

                for (CartItem cartItem : cartItems) {
                    int foodId = cartItem.getFoodItem().getFoodId();
                    FoodItem locked = orderDAO.lockFoodItemForUpdate(conn, foodId);

                    if (locked == null) {
                        throw new RuntimeException("Food item no longer exists: " +
                                cartItem.getFoodItem().getFoodName());
                    }
                    if (!locked.isAvailable()) {
                        throw new RuntimeException("'" + locked.getFoodName() +
                                "' is no longer available.");
                    }
                    if (locked.getStockQuantity() < cartItem.getQuantity()) {
                        throw new RuntimeException("Insufficient stock for '" + locked.getFoodName() +
                                "'. Requested: " + cartItem.getQuantity() +
                                ", Available: " + locked.getStockQuantity());
                    }

                    OrderItem oi = new OrderItem(
                            foodId,
                            locked.getFoodName(),
                            locked.getPrice(),
                            cartItem.getQuantity());
                    orderItems.add(oi);
                    total += oi.getSubtotal();
                }

                // Step 3: Insert order header
                Order order = new Order(0, customer, LocalDateTime.now(),
                        new ArrayList<>(orderItems), total, "Pending");
                int orderId = orderDAO.insertOrderHeader(conn, order);

                // Step 4: Insert order items
                orderDAO.insertOrderItems(conn, orderId, orderItems);

                // Steps 5 & 6: Deduct stock and update availability
                for (CartItem cartItem : cartItems) {
                    int foodId = cartItem.getFoodItem().getFoodId();
                    boolean deducted = orderDAO.deductStock(conn, foodId, cartItem.getQuantity());
                    if (!deducted) {
                        throw new RuntimeException("Stock deduction failed for food_id=" + foodId +
                                " \u2014 concurrent modification detected.");
                    }
                    orderDAO.updateAvailabilityIfEmpty(conn, foodId);
                }

                // Step 7: Commit
                conn.commit();
                logger.info("Order " + orderId + " placed successfully.");
                return new Order(orderId, customer, order.getOrderDate(),
                        new ArrayList<>(orderItems), total, "Pending");

            } catch (Exception e) {
                try { conn.rollback(); } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Rollback failed.", ex);
                }
                logger.log(Level.WARNING, "Order placement rolled back: " + e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException e) {
                    logger.log(Level.WARNING, "setAutoCommit(true) failed.", e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during order placement: " + e.getMessage(), e);
        }
    }
    public static List<Order> getOrdersByCustomer(int customerId) {
        List<Order> orders = orderDAO.findByCustomerId(customerId);
        for (Order o : orders) {
            o.getOrderItems().addAll(orderDAO.findItemsByOrderId(o.getOrderId()));
        }
        return orders;
    }

    public static List<Order> getAllOrders() {
        List<Order> orders = orderDAO.findAll();
        for (Order o : orders) {
            o.getOrderItems().addAll(orderDAO.findItemsByOrderId(o.getOrderId()));
        }
        return orders;
    }

    /**
     * Updates the status of an order, enforcing a controlled transition workflow.
     * Allowed transitions:
     *   Pending    \u2192 Preparing | Cancelled
     *   Preparing  \u2192 Ready     | Cancelled
     *   Ready      \u2192 Completed
     *   Completed  \u2192 (terminal)
     *   Cancelled  \u2192 (terminal)
     *
     * Cancelling an order restores stock and re-enables availability in a transaction.
     */
    public static boolean updateOrderStatus(int orderId, String newStatus) {
        String current = orderDAO.getStatusById(orderId);
        if (current == null) {
            throw new RuntimeException("Order " + orderId + " not found.");
        }
        if (!isValidTransition(current, newStatus)) {
            throw new RuntimeException(
                    "Invalid status transition: " + current + " -> " + newStatus);
        }

        if ("Cancelled".equals(newStatus) && !"Cancelled".equals(current)) {
            orderDAO.restoreStockAndAvailability(orderId);
        }

        return orderDAO.updateStatus(orderId, newStatus);
    }

    public static int countPendingOrders() {
        return orderDAO.countPending();
    }

    private static boolean isValidTransition(String from, String to) {
        return switch (from) {
            case "Pending"   -> "Preparing".equals(to) || "Cancelled".equals(to);
            case "Preparing" -> "Ready".equals(to)     || "Cancelled".equals(to);
            case "Ready"     -> "Completed".equals(to);
            default          -> false;   // Completed and Cancelled are terminal
        };
    }
}
