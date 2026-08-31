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
        String sql = "SELECT customer_id, full_name, username, password_hash, created_at, " +
                     "email, phone_number " +
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
        String sql = "SELECT customer_id, full_name, username, password_hash, created_at, " +
                     "email, phone_number " +
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
    public boolean updateCustomerUsername(int customerId, String username) {
        String sql = "UPDATE customers SET username = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.updateCustomerUsername failed.", e);
            throw new RuntimeException("Could not update customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateCustomerUsernameAndPassword(int customerId, String username,
                                                      String passwordHash) {
        String sql = "UPDATE customers SET username = ?, password_hash = ? " +
                     "WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setInt(3, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.updateCustomerUsernameAndPassword failed.", e);
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

    // ── Customer self-service profile ─────────────────────────────────────────

    @Override
    public Customer getProfileById(int customerId) {
        String sql =
            "SELECT customer_id, full_name, username, password_hash, created_at, " +
            "email, phone_number, date_of_birth, profile_image_path, delivery_address " +
            "FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProfile(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.getProfileById failed.", e);
            throw new RuntimeException("Could not fetch customer profile: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean updateProfile(int customerId, String fullName, String email,
                                 String phoneNumber, java.time.LocalDate dateOfBirth,
                                 String profileImagePath, String deliveryAddress) {
        String sql =
            "UPDATE customers SET full_name = ?, email = ?, phone_number = ?, " +
            "date_of_birth = ?, profile_image_path = ?, delivery_address = ? " +
            "WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phoneNumber);
            if (dateOfBirth != null) {
                ps.setDate(4, java.sql.Date.valueOf(dateOfBirth));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            ps.setString(5, profileImagePath);
            ps.setString(6, deliveryAddress);
            ps.setInt(7, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "CustomerDAO.updateProfile failed.", e);
            throw new RuntimeException("Could not update customer profile: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmailCaseInsensitiveExcludingCustomer(String email,
                                                                  int excludeCustomerId) {
        String sql = "SELECT COUNT(*) FROM customers " +
                     "WHERE email IS NOT NULL AND LOWER(email) = LOWER(?) " +
                     "AND customer_id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeCustomerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                "CustomerDAO.existsByEmailCaseInsensitiveExcludingCustomer failed.", e);
            throw new RuntimeException("Could not check email uniqueness: " + e.getMessage(), e);
        }
        return false;
    }

    // ── Private mappers ───────────────────────────────────────────────────────

    /** Maps a ResultSet row that includes the created_at, email, and phone_number columns. */
    private static Customer mapFull(ResultSet rs) throws SQLException {
        Customer c = new Customer(
            rs.getInt("customer_id"),
            rs.getString("full_name"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("created_at")
        );
        c.setEmail(rs.getString("email"));
        c.setPhoneNumber(rs.getString("phone_number"));
        return c;
    }

    /** Maps a ResultSet row that includes all profile columns. */
    private static Customer mapProfile(ResultSet rs) throws SQLException {
        java.sql.Date sqlDate = rs.getDate("date_of_birth");
        java.time.LocalDate dob = (sqlDate != null) ? sqlDate.toLocalDate() : null;
        return new Customer(
            rs.getInt("customer_id"),
            rs.getString("full_name"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("created_at"),
            rs.getString("email"),
            rs.getString("phone_number"),
            dob,
            rs.getString("profile_image_path"),
            rs.getString("delivery_address")
        );
    }
}
