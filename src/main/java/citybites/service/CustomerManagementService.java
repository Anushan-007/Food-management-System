package citybites.service;

import citybites.dao.CustomerDAO;
import citybites.dao.impl.CustomerDAOImpl;
import citybites.model.Customer;
import citybites.util.PasswordValidator;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Admin-side service for managing customer accounts.
 *
 * <p>All mutating operations validate their inputs and throw
 * {@link IllegalArgumentException} for validation failures or
 * {@link IllegalStateException} for integrity constraint violations
 * (e.g. deleting a customer who has orders).
 */
public class CustomerManagementService {

    private static final CustomerDAO dao = new CustomerDAOImpl();

    public static final int MIN_FULLNAME_LENGTH = 2;
    public static final int MAX_FULLNAME_LENGTH = 100;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;

    private CustomerManagementService() {}

    // ── Queries ─────────────────────────────────────────────────────────────

    /** Returns all customers ordered by registration date (newest first). */
    public static List<Customer> getAllCustomers() {
        return dao.getAll();
    }

    /**
     * Returns the customer with the given ID, or {@code null} if not found.
     * The returned Customer includes the {@code createdAt} timestamp.
     */
    public static Customer getCustomerById(int id) {
        return dao.getById(id);
    }

    /**
     * Returns the full profile (all nullable profile columns populated) for
     * the given customer ID. Intended for the admin "View Details" action.
     *
     * <p>Unlike {@link #getCustomerById}, this method fetches email, phone,
     * date_of_birth, profile_image_path, and delivery_address.
     *
     * @param customerId the ID of the customer to look up — must be positive
     * @return the fully-populated {@link Customer}
     * @throws IllegalArgumentException if {@code customerId} is not positive
     * @throws IllegalStateException    if no customer with that ID exists
     */
    public static Customer getCustomerDetails(int customerId) {
        if (customerId < 1) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }
        Customer c = dao.getProfileById(customerId);
        if (c == null) {
            throw new IllegalStateException("Customer record no longer exists.");
        }
        return c;
    }

    /** Returns {@code true} if the customer has at least one order. */
    public static boolean customerHasOrders(int id) {
        return dao.hasOrders(id);
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    /**
     * Validates and inserts a new customer with a BCrypt-hashed password.
     *
     * @return the generated {@code customer_id}
     * @throws IllegalArgumentException for any validation failure
     */
    public static int addCustomer(String fullName, String username,
                                   String password, String confirmPassword) {
        String trimName = (fullName == null) ? "" : fullName.trim();
        String trimUser = (username == null) ? "" : username.trim();

        validateName(trimName);
        validateUsername(trimUser);
        if (dao.existsByUsernameCaseInsensitiveExcludingId(trimUser, -1)) {
            throw new IllegalArgumentException("A customer with this username already exists.");
        }
        validatePassword(password, confirmPassword);

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return dao.insertCustomer(trimName, trimUser, hash);
    }

    /**
     * Validates and updates the admin-editable fields for a customer.
     *
     * <p>Ownership rules — admin update scope:
     * <ul>
     *   <li><strong>Username</strong> — always updated.</li>
     *   <li><strong>Password hash</strong> — updated only when {@code newPassword}
     *       is non-null and non-empty.</li>
     * </ul>
     *
     * <p>The following fields are <em>not</em> updated here; customers own them
     * through their "My Profile" screen:
     * full_name, email, phone_number, date_of_birth, profile_image_path, delivery_address.
     *
     * <p>The {@code fullName} parameter is accepted for API compatibility and is
     * displayed in the form for identification, but is <strong>not written to the
     * database</strong> by this method.
     *
     * @return {@code true} if the update succeeded
     * @throws IllegalArgumentException for any validation failure
     */
    public static boolean updateCustomer(int id, String fullName, String username,
                                          String newPassword, String confirmPassword) {
        // fullName is intentionally not persisted — customers own their own name.
        String trimUser = (username == null) ? "" : username.trim();

        validateUsername(trimUser);
        if (dao.existsByUsernameCaseInsensitiveExcludingId(trimUser, id)) {
            throw new IllegalArgumentException("A customer with this username already exists.");
        }

        boolean changePassword = (newPassword != null && !newPassword.isEmpty());
        if (changePassword) {
            validatePassword(newPassword, confirmPassword);
            String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            return dao.updateCustomerUsernameAndPassword(id, trimUser, hash);
        } else {
            return dao.updateCustomerUsername(id, trimUser);
        }
    }

    /**
     * Deletes the customer with the given ID.
     *
     * @throws IllegalStateException if the customer has existing orders
     */
    public static boolean deleteCustomer(int id) {
        if (dao.hasOrders(id)) {
            throw new IllegalStateException(
                "This customer has existing orders and cannot be deleted.");
        }
        return dao.deleteCustomer(id);
    }

    // ── Private validators ───────────────────────────────────────────────────

    private static void validateName(String trimName) {
        if (trimName.length() < MIN_FULLNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Full name is required (minimum " + MIN_FULLNAME_LENGTH + " characters).");
        }
        if (trimName.length() > MAX_FULLNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Full name must not exceed " + MAX_FULLNAME_LENGTH + " characters.");
        }
    }

    private static void validateUsername(String trimUser) {
        if (trimUser.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Username is required (minimum " + MIN_USERNAME_LENGTH + " characters).");
        }
        if (trimUser.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                "Username must not exceed " + MAX_USERNAME_LENGTH + " characters.");
        }
    }

    private static void validatePassword(String password, String confirm) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (!PasswordValidator.isCompliant(password)) {
            throw new IllegalArgumentException(PasswordValidator.POLICY_MESSAGE);
        }
    }
}
