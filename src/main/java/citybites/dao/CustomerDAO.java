package citybites.dao;

import citybites.model.Customer;
import java.util.Optional;

/**
 * Data-access contract for the customers table.
 */
public interface CustomerDAO {

    boolean insert(String fullName, String username, String passwordHash);

    Optional<Customer> findByUsername(String username);

    boolean usernameExists(String username);

    int countAll();
}
