package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.OrderDAO;
import citybites.model.Customer;
import citybites.model.Order;
import citybites.model.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderDAOImpl implements OrderDAO {

    private static final Logger logger = Logger.getLogger(OrderDAOImpl.class.getName());

    /**
     * Inserts the order and all its items in a single transaction.
     * Returns the generated order_id, or -1 on failure.
     */
    @Override
    public int insertOrder(Order order) {
        Connection conn = DatabaseConnection.get();
        try {
            conn.setAutoCommit(false);

            // 1. Insert the order header
            int orderId;
            String orderSql = "INSERT INTO orders (customer_id, order_date, total_amount, status) " +
                               "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(
                    orderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getCustomer().getCustomerId());
                ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
                ps.setDouble(3, order.getTotalAmount());
                ps.setString(4, order.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No generated key for order.");
                    orderId = keys.getInt(1);
                }
            }

            // 2. Insert each order item
            String itemSql = "INSERT INTO order_items " +
                              "(order_id, food_id, food_name, unit_price, quantity) " +
                              "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (OrderItem item : order.getOrderItems()) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getFoodId());
                    ps.setString(3, item.getFoodName());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return orderId;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.insertOrder failed; rolling back.", e);
            try { conn.rollback(); } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Rollback failed.", ex);
            }
            throw new RuntimeException("Could not place order: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {
                logger.log(Level.WARNING, "setAutoCommit(true) failed.", e);
            }
        }
    }

    @Override
    public List<Order> findByCustomerId(int customerId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, o.order_date, o.total_amount, o.status, " +
                     "       c.full_name, c.username " +
                     "FROM orders o " +
                     "JOIN customers c ON c.customer_id = o.customer_id " +
                     "WHERE o.customer_id = ? " +
                     "ORDER BY o.order_date DESC";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(mapOrder(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.findByCustomerId failed.", e);
        }
        return orders;
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, o.order_date, o.total_amount, o.status, " +
                     "       c.full_name, c.username " +
                     "FROM orders o " +
                     "JOIN customers c ON c.customer_id = o.customer_id " +
                     "ORDER BY o.order_date DESC";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) orders.add(mapOrder(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.findAll failed.", e);
        }
        return orders;
    }

    @Override
    public boolean updateStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.updateStatus failed.", e);
            throw new RuntimeException("Could not update order status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderItem> findItemsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT food_id, food_name, unit_price, quantity " +
                     "FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getDouble("unit_price"),
                            rs.getInt("quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.findItemsByOrderId failed.", e);
        }
        return items;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Order mapOrder(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                ""   // password hash not needed in order context
        );
        // Order items are lazy-loaded via findItemsByOrderId when needed
        return new Order(
                rs.getInt("order_id"),
                customer,
                rs.getTimestamp("order_date").toLocalDateTime(),
                new ArrayList<>(),
                rs.getDouble("total_amount"),
                rs.getString("status")
        );
    }
}
