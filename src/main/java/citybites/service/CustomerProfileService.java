package citybites.service;

import citybites.dao.CustomerDAO;
import citybites.dao.impl.CustomerDAOImpl;
import citybites.model.Customer;
import citybites.util.SessionManager;
import java.time.LocalDate;
import java.time.Period;
import java.util.logging.Logger;

/**
 * Business logic for the customer self-service profile feature.
 *
 * <h2>Authorization</h2>
 * <p>The <em>customer-facing</em> entry points are:
 * <ul>
 *   <li>{@link #getCurrentCustomerProfile()} — reads the logged-in customer's ID
 *       from {@link SessionManager}; never accepts an ID from UI state.</li>
 *   <li>{@link #updateCurrentCustomerProfile} — same; rejects calls when no
 *       customer session is active.</li>
 * </ul>
 *
 * <p>The ID-based overloads ({@code getProfile(int)}, {@code updateProfile(int, ...)})
 * are retained for admin-side and test use only. They must not be called from
 * customer-facing UI.
 *
 * <h2>Email uniqueness</h2>
 * <p>When a non-blank email is supplied, {@code updateProfile} performs a
 * case-insensitive uniqueness check that excludes the updating customer's own row.
 * Blank email is normalised to {@code null} and skips the uniqueness query.
 *
 * <h2>Password changes</h2>
 * <p>Not handled here; see {@code AuthService}.
 */
public class CustomerProfileService {

    private static final Logger     logger = Logger.getLogger(CustomerProfileService.class.getName());
    private static final CustomerDAO dao    = new CustomerDAOImpl();

    private CustomerProfileService() {}

    // ── Session-aware customer entry points ───────────────────────────────────

    /**
     * Returns the full profile for the currently logged-in customer, or
     * {@code null} when no customer session is active or the row is not found.
     */
    public static Customer getCurrentCustomerProfile() {
        Customer session = SessionManager.getLoggedInCustomer();
        if (session == null) return null;
        return dao.getProfileById(session.getCustomerId());
    }

    /**
     * Validates and saves profile changes for the currently logged-in customer.
     * Derives the customer ID exclusively from {@link SessionManager}.
     *
     * @return {@code null} on success, or a human-readable error message on failure
     */
    public static String updateCurrentCustomerProfile(String fullName, String email,
                                                       String phoneNumber, LocalDate dateOfBirth,
                                                       String profileImagePath,
                                                       String deliveryAddress) {
        Customer session = SessionManager.getLoggedInCustomer();
        if (session == null) {
            return "No customer is currently logged in.";
        }
        return updateProfile(session.getCustomerId(), fullName, email, phoneNumber,
                             dateOfBirth, profileImagePath, deliveryAddress);
    }

    // ── ID-based methods (admin / test use) ───────────────────────────────────

    /**
     * Returns the full profile (all nullable profile columns populated) for the given
     * customer ID, or {@code null} if the customer does not exist.
     *
     * <p><strong>Note:</strong> customer-facing UI must call
     * {@link #getCurrentCustomerProfile()} instead.
     */
    public static Customer getProfile(int customerId) {
        return dao.getProfileById(customerId);
    }

    /**
     * Validates and saves profile changes for the given customer ID.
     * {@code password_hash} is never modified.
     *
     * <p><strong>Note:</strong> customer-facing UI must call
     * {@link #updateCurrentCustomerProfile} instead.
     *
     * @return {@code null} on success, or a human-readable error message on failure
     */
    public static String updateProfile(int customerId, String fullName, String email,
                                       String phoneNumber, LocalDate dateOfBirth,
                                       String profileImagePath, String deliveryAddress) {

        // ── Full name (required) ────────────────────────────────────────────
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name is required.";
        }
        if (fullName.trim().length() > 100) {
            return "Full name must not exceed 100 characters.";
        }

        // ── Email (optional) ────────────────────────────────────────────────
        if (email != null && !email.trim().isEmpty()) {
            if (email.trim().length() > 150) {
                return "Email must not exceed 150 characters.";
            }
            if (!isValidEmail(email.trim())) {
                return "Email address is not valid.";
            }
            // Case-insensitive uniqueness — allow the customer to keep their own address
            if (dao.existsByEmailCaseInsensitiveExcludingCustomer(email.trim(), customerId)) {
                return "A customer with this email already exists.";
            }
        }

        // ── Phone number (optional) ─────────────────────────────────────────
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            if (!isValidPhone(phoneNumber.trim())) {
                return "Phone number may only contain digits, spaces, +, -, or () — max 20 chars.";
            }
        }

        // ── Date of birth (optional) ────────────────────────────────────────
        if (dateOfBirth != null) {
            if (dateOfBirth.isAfter(LocalDate.now())) {
                return "Date of birth cannot be in the future.";
            }
            if (Period.between(dateOfBirth, LocalDate.now()).getYears() > 120) {
                return "Date of birth is unrealistically old (over 120 years).";
            }
        }

        // ── Normalise optional string fields (blank → null) ─────────────────
        String normEmail   = notBlank(email)            ? email.trim()            : null;
        String normPhone   = notBlank(phoneNumber)      ? phoneNumber.trim()      : null;
        String normAddress = notBlank(deliveryAddress)  ? deliveryAddress.trim()  : null;
        String normImg     = notBlank(profileImagePath) ? profileImagePath.trim() : null;

        boolean ok = dao.updateProfile(customerId, fullName.trim(),
                normEmail, normPhone, dateOfBirth, normImg, normAddress);
        return ok ? null : "Profile update failed — please try again.";
    }

    // ── Age Calculation ───────────────────────────────────────────────────────

    /**
     * Returns the customer's age in whole completed years from the given date of birth.
     * Returns {@code -1} if {@code dob} is {@code null}.
     */
    public static int calculateAge(LocalDate dob) {
        if (dob == null) return -1;
        return Period.between(dob, LocalDate.now()).getYears();
    }

    // ── Validation helpers (public for tests) ─────────────────────────────────

    public static boolean isValidEmail(String email) {
        // local@domain.tld — simple pattern suitable for a desktop app
        return email.matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[A-Za-z]{2,}$");
    }

    public static boolean isValidPhone(String phone) {
        return phone.length() <= 20 && phone.matches("^[0-9 +\\-()]+$");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
