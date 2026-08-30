package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.OrderDAO;
import citybites.model.Customer;
import citybites.model.FoodItem;
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

    // ── Transaction-aware methods ─────────────────────────────────────────────────────────────────────

    @Override
    public int insertOrderHeader(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO orders (customer_id, order_date, total_amount, status) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,       order.getCustomer().getCustomerId());
            ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            ps.setDouble(3,    order.getTotalAmount());
            ps.setString(4,    order.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for order.");
                return keys.getInt(1);
            }
        }
    }

    @Override
    public void insertOrderItems(Connection conn, int orderId, List<OrderItem> items)
            throws SQLException {
        String sql = "INSERT INTO order_items " +
                     "(order_id, food_id, food_name, unit_price, quantity) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OrderItem item : items) {
                ps.setInt(1,    orderId);
                ps.setInt(2,    item.getFoodId());
                ps.setString(3, item.getFoodName());
                ps.setDouble(4, item.getUnitPrice());
                ps.setInt(5,    item.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public FoodItem lockFoodItemForUpdate(Connection conn, int foodId) throws SQLException {
        String sql = "SELECT food_id, food_name, price, available, stock_quantity, image_path " +
                     "FROM food_items WHERE food_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapFoodItem(rs);
            }
        }
        return null;
    }

    @Override
    public boolean deductStock(Connection conn, int foodId, int quantity) throws SQLException {
        String sql = "UPDATE food_items " +
                     "SET stock_quantity = stock_quantity - ? " +
                     "WHERE food_id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, foodId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public void updateAvailabilityIfEmpty(Connection conn, int foodId) throws SQLException {
        String sql = "UPDATE food_items SET available = 0 " +
                     "WHERE food_id = ? AND stock_quantity = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            ps.executeUpdate();
        }
    }
    // ── Standard reads ───────────────────────────────────────────────────────────────────────────────────────────────────

    @Override
    public List<Order> findByCustomerId(int customerId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, o.order_date, o.total_amount, o.status, " +
                     "       c.full_name, c.username " +
                     "FROM orders o JOIN customers c ON c.customer_id = o.customer_id " +
                     "WHERE o.customer_id = ? ORDER BY o.order_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
                     "FROM orders o JOIN customers c ON c.customer_id = o.customer_id " +
                     "ORDER BY o.order_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) orders.add(mapOrder(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.findAll failed.", e);
        }
        return orders;
    }

    @Override
    public List<OrderItem> findItemsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT food_id, food_name, unit_price, quantity " +
                     "FROM order_items WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getDouble("unit_price"),
                            rs.getInt("quantity")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.findItemsByOrderId failed.", e);
        }
        return items;
    }

    @Override
    public String getStatusById(int orderId) {
        String sql = "SELECT status FROM orders WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("status");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.getStatusById failed.", e);
        }
        return null;
    }

    @Override
    public int countPending() {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 'Pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.countPending failed.", e);
        }
        return 0;
    }
    // ── Status management ────────────────────────────────────────────────────────────────────────────────────────

    @Override
    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "OrderDAO.updateStatus failed.", e);
            throw new RuntimeException("Could not update order status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean restoreStockAndAvailability(int orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Fetch order items
                String itemSql = "SELECT food_id, quantity FROM order_items WHERE order_id = ?";
                List<int[]> items = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            items.add(new int[]{rs.getInt("food_id"), rs.getInt("quantity")});
                        }
                    }
                }

                // Restore stock
                String restoreSql = "UPDATE food_items SET stock_quantity = stock_quantity + ? " +
                                    "WHERE food_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(restoreSql)) {
                    for (int[] item : items) {
                        ps.setInt(1, item[1]);
                        ps.setInt(2, item[0]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Re-enable availability where stock > 0
                String availSql = "UPDATE food_items SET available = 1 " +
                                  "WHERE food_id = ? AND stock_quantity > 0";
                try (PreparedStatement ps = conn.prepareStatement(availSql)) {
                    for (int[] item : items) {
                        ps.setInt(1, item[0]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "restoreStockAndAvailability rollback.", e);
                throw new RuntimeException("Could not restore stock: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "restoreStockAndAvailability connection error.", e);
            throw new RuntimeException("Database error restoring stock: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────────────────────────────────

    private Order mapOrder(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                "");
        return new Order(
                rs.getInt("order_id"),
                customer,
                rs.getTimestamp("order_date").toLocalDateTime(),
                new ArrayList<>(),
                rs.getDouble("total_amount"),
                rs.getString("status"));
    }

    private FoodItem mapFoodItem(ResultSet rs) throws SQLException {
        FoodItem f = new FoodItem();
        f.setFoodId(rs.getInt("food_id"));
        f.setFoodName(rs.getString("food_name"));
        f.setPrice(rs.getDouble("price"));
        f.setAvailable(rs.getBoolean("available"));
        f.setStockQuantity(rs.getInt("stock_quantity"));
        f.setImagePath(rs.getString("image_path"));
        return f;
    }
}
