package citybites.service;

import citybites.dao.OrderDAO;
import citybites.dao.impl.OrderDAOImpl;
import citybites.model.CartItem;
import citybites.model.Customer;
import citybites.model.Order;
import citybites.model.OrderItem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles order placement, retrieval and status updates.
 */
public class OrderService {

    private static final OrderDAO orderDAO = new OrderDAOImpl();

    private OrderService() {}

    /**
     * Converts the customer's cart into a persisted order.
     *
     * @param customer  the logged-in customer
     * @param cartItems the items currently in the cart
     * @return the newly created Order (with its generated ID), or null on failure
     */
    public static Order placeOrder(Customer customer, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart.");
        }

        double totalAmount = 0;
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem oi = new OrderItem(
                    cartItem.getFoodItem().getFoodId(),
                    cartItem.getFoodItem().getFoodName(),
                    cartItem.getFoodItem().getPrice(),
                    cartItem.getQuantity()
            );
            orderItems.add(oi);
            totalAmount += oi.getSubtotal();
        }

        Order order = new Order(
                0,                      // ID will be assigned by the database
                customer,
                LocalDateTime.now(),
                orderItems,
                totalAmount,
                "Pending"
        );

        int generatedId = orderDAO.insertOrder(order);
        if (generatedId < 0) return null;

        return new Order(generatedId, customer, order.getOrderDate(),
                         orderItems, totalAmount, "Pending");
    }

    /** Returns all orders belonging to the given customer. */
    public static List<Order> getOrdersByCustomer(int customerId) {
        List<Order> orders = orderDAO.findByCustomerId(customerId);
        // Eagerly load order items
        for (Order o : orders) {
            o.getOrderItems().addAll(orderDAO.findItemsByOrderId(o.getOrderId()));
        }
        return orders;
    }

    /** Returns every order in the system (for admin view). */
    public static List<Order> getAllOrders() {
        List<Order> orders = orderDAO.findAll();
        // Eagerly load order items
        for (Order o : orders) {
            o.getOrderItems().addAll(orderDAO.findItemsByOrderId(o.getOrderId()));
        }
        return orders;
    }

    /** Updates the status of a specific order. */
    public static boolean updateOrderStatus(int orderId, String status) {
        return orderDAO.updateStatus(orderId, status);
    }
}
