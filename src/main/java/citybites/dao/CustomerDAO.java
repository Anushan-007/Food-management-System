package citybites.dao;

import citybites.model.Customer;
import java.util.Optional;

/**
 * Data-access contract for the customers table.
 */
public interface CustomerDAO {

    /**
     * Inserts a new customer.  Returns true on success.
     * Throws RuntimeException if username already exists.
     */
    boolean insert(String fullName, String username, String passwordHash);

    /** Looks up a customer by username (case-insensitive). */
    Optional<Customer> findByUsername(String username);

    /** Checks whether a username is already taken. */
    boolean usernameExists(String username);
}
