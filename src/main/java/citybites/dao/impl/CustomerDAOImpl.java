package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.CustomerDAO;
import citybites.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerDAOImpl implements CustomerDAO {

    private static final Logger logger = Logger.getLogger(CustomerDAOImpl.class.getName());

    // ── Existing methods ─────────────────────────────────────────────────────

    @Override
    public boolean insert(String fullName, String username, String passwordHash) {
        String sql = "INSERT INTO customers (full_name, username, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.insert failed.", e);
            throw new RuntimeException("Could not register customer: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findByUsername(String username) {
        String sql = "SELECT customer_id, full_name, username, password_hash " +
                     "FROM customers WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("password_hash")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.findByUsername failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM customers WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.usernameExists failed.", e);
        }
        return false;
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM customers";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.countAll failed.", e);
        }
        return 0;
    }

    // ── Admin management extensions ───────────────────────────────────────────

    @Override
    public List<Customer> getAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, full_name, username, password_hash, created_at " +
                     "FROM customers ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapFull(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.getAll failed.", e);
            throw new RuntimeException("Could not load customers: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Customer getById(int customerId) {
        String sql = "SELECT customer_id, full_name, username, password_hash, created_at " +
                     "FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapFull(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.getById failed.", e);
            throw new RuntimeException("Could not fetch customer: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean existsByUsernameCaseInsensitiveExcludingId(String username, int excludeId) {
        String sql = "SELECT COUNT(*) FROM customers " +
                     "WHERE LOWER(username) = LOWER(?) AND customer_id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);   // -1 never matches a real PK (positive)
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.existsByUsernameCaseInsensitiveExcludingId failed.", e);
            throw new RuntimeException("Could not check username uniqueness: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public int insertCustomer(String fullName, String username, String passwordHash) {
        String sql = "INSERT INTO customers (full_name, username, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.insertCustomer failed.", e);
            throw new RuntimeException("Could not add customer: " + e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean updateCustomerProfile(int customerId, String fullName, String username) {
        String sql = "UPDATE customers SET full_name = ?, username = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setInt(3, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.updateCustomerProfile failed.", e);
            throw new RuntimeException("Could not update customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateCustomerWithPassword(int customerId, String fullName,
                                               String username, String passwordHash) {
        String sql = "UPDATE customers SET full_name = ?, username = ?, password_hash = ? " +
                     "WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setInt(4, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.updateCustomerWithPassword failed.", e);
            throw new RuntimeException("Could not update customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteCustomer(int customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.deleteCustomer failed.", e);
            throw new RuntimeException("Could not delete customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasOrders(int customerId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.hasOrders failed — failing safe.", e);
            return true;  // fail-safe: prevent deletion on DB error
        }
        return false;
    }

    // ── Private mapper ────────────────────────────────────────────────────────

    /** Maps a ResultSet row that includes the created_at column. */
    private static Customer mapFull(ResultSet rs) throws SQLException {
        return new Customer(
            rs.getInt("customer_id"),
            rs.getString("full_name"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("created_at")
        );
    }
}
