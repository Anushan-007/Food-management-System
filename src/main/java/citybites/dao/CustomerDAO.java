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
     * Updates <strong>only the username</strong> for the given customer ID.
     * Full name, password_hash, and all customer-owned profile columns are unchanged.
     *
     * @return {@code true} if exactly one row was updated
     */
    boolean updateCustomerUsername(int customerId, String username);

    /**
     * Updates the username <em>and</em> password_hash for the given customer ID.
     * Full name and all customer-owned profile columns are unchanged.
     *
     * @return {@code true} if exactly one row was updated
     */
    boolean updateCustomerUsernameAndPassword(int customerId, String username, String passwordHash);

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

    // ── Customer self-service profile ─────────────────────────────────────────

    /**
     * Returns the customer with all profile columns (email, phone_number, date_of_birth,
     * profile_image_path, delivery_address) populated for the given ID,
     * or {@code null} if not found.
     */
    Customer getProfileById(int customerId);

    /**
     * Updates the editable profile fields for the customer with the given ID.
     * {@code password_hash} and {@code created_at} are never touched.
     *
     * @return {@code true} if exactly one row was updated
     */
    boolean updateProfile(int customerId, String fullName, String email, String phoneNumber,
                          java.time.LocalDate dateOfBirth, String profileImagePath,
                          String deliveryAddress);

    /**
     * Returns {@code true} if any customer row — excluding the row with
     * {@code excludeCustomerId} — has an {@code email} that matches
     * {@code email} case-insensitively.
     *
     * <p>Pass {@code excludeCustomerId = -1} to check all rows (add-mode).
     * Only call this when {@code email} is non-null and non-blank.
     */
    boolean existsByEmailCaseInsensitiveExcludingCustomer(String email, int excludeCustomerId);
}
