package citybites.util;

/**
 * Reusable password-policy validator.
 *
 * Policy: minimum 8 characters, containing at least one letter and one digit.
 * Applied at registration time only; never gates existing BCrypt logins.
 */
public final class PasswordValidator {

    private PasswordValidator() {}

    /** Human-readable policy description for use in validation error dialogs. */
    public static final String POLICY_MESSAGE =
        "Password must contain at least 8 characters, including one letter and one number.";

    /**
     * Returns {@code true} when {@code password} satisfies the registration policy.
     * Returns {@code false} for {@code null}, empty, too-short, or letter/digit-absent input.
     */
    public static boolean isCompliant(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }
}
