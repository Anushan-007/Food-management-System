package citybites.service;

import citybites.config.DatabaseConnection;
import citybites.dao.CustomerDAO;
import citybites.dao.impl.CustomerDAOImpl;
import citybites.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Handles all authentication and registration logic.
 */
public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static final CustomerDAO customerDAO = new CustomerDAOImpl();

    private AuthService() {}

    public static boolean adminLogin(String username, String password) {
        if (username == null || password == null) return false;
        String sql = "SELECT password_hash FROM admins WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return BCrypt.checkpw(password, rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "AdminLogin DB error.", e);
        }
        return false;
    }

    public static Optional<Customer> customerLogin(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        Optional<Customer> opt = customerDAO.findByUsername(username.trim());
        if (opt.isEmpty()) return Optional.empty();
        Customer c = opt.get();
        if (BCrypt.checkpw(password, c.getPassword())) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    public static boolean register(String fullName, String username, String password) {
        username = username.trim();
        if (customerDAO.usernameExists(username)) {
            throw new RuntimeException("Username '" + username + "' is already taken.");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return customerDAO.insert(fullName, username, hash);
    }

    public static int countCustomers() {
        return customerDAO.countAll();
    }
}
