package citybites.dao;

import citybites.model.Customer;
import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for the customers table.
 */
public interface CustomerDAO {

    // ── Existing (used by AuthService) ──────────────────────────────────────

    boolean insert(String fullName, String username, String passwordHash);

    Optional<Customer> findByUsername(String username);

    boolean usernameExists(String username);

    int countAll();

    // ── Admin management extensions ─────────────────────────────────────────

    /** Returns all customers ordered by registration date descending. */
    List<Customer> getAll();

    /**
     * Returns the customer with the given ID, or {@code null} if not found.
     * The returned Customer includes the {@code createdAt} timestamp.
     */
    Customer getById(int customerId);

    /**
     * Returns {@code true} if any customer row (excluding {@code excludeId}) has
     * a username that matches {@code username} case-insensitively.
     * Pass {@code excludeId = -1} to check all rows (add mode).
     */
    boolean existsByUsernameCaseInsensitiveExcludingId(String username, int excludeId);

    /**
     * Inserts a new customer row and returns the generated {@code customer_id}.
     * Returns {@code -1} if the insert did not return a generated key.
     */
    int insertCustomer(String fullName, String username, String passwordHash);

    /**
     * Updates the full_name and username of the customer with the given ID,
     * leaving password_hash unchanged.
     *
     * @return {@code true} if exactly one row was updated
     */
    boolean updateCustomerProfile(int customerId, String fullName, String username);

    /**
     * Updates full_name, username, and password_hash of the customer with the given ID.
     *
     * @return {@code true} if exactly one row was updated
     */
    boolean updateCustomerWithPassword(int customerId, String fullName,
                                       String username, String passwordHash);

    /**
     * Deletes the customer row with the given ID.
     *
     * @return {@code true} if exactly one row was deleted
     * @throws RuntimeException wrapping {@code SQLException} if the delete fails
     *         (e.g. ON DELETE RESTRICT violation when orders exist)
     */
    boolean deleteCustomer(int customerId);

    /**
     * Returns {@code true} if the customer has at least one row in the orders table.
     * Used by {@code CustomerManagementService} to guard deletion.
     */
    boolean hasOrders(int customerId);
}
