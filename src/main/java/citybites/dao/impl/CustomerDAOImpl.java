package citybites.dao.impl;

import citybites.config.DatabaseConnection;
import citybites.dao.CustomerDAO;
import citybites.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerDAOImpl implements CustomerDAO {

    private static final Logger logger = Logger.getLogger(CustomerDAOImpl.class.getName());

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
}
