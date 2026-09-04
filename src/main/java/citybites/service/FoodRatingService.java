package citybites.service;

import citybites.dao.FoodRatingDAO;
import citybites.dao.impl.FoodRatingDAOImpl;
import citybites.model.Customer;
import citybites.model.FoodRating;
import citybites.model.FoodRatingSummary;
import citybites.model.FoodReviewDetail;
import citybites.util.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer-facing service for food ratings.
 *
 * <p><b>Authorization design:</b> The customer ID is always read from
 * {@link SessionManager}. The UI is never allowed to pass an arbitrary customer ID.
 * The DAO enforces ownership and order-status rules atomically inside a single
 * JDBC transaction, so the order cannot become ineligible between validation and save.
 */
public class FoodRatingService {

    private static final int MAX_REVIEW_LENGTH = 500;
    private static final FoodRatingDAO dao = new FoodRatingDAOImpl();

    private FoodRatingService() {}

    /** Returns the existing rating for one order item, or empty if not yet rated. */
    public static Optional<FoodRating> getRatingForOrderItem(int orderItemId) {
        return dao.findByOrderItemId(orderItemId);
    }

    /**
     * Returns all existing ratings for items in the given order.
     * Items without a rating are omitted (they return no row from food_ratings).
     */
    public static List<FoodRating> getRatingsForOrder(int orderId) {
        return dao.findByOrderId(orderId);
    }

    /**
     * Saves (inserts or updates) a rating for the given order item on behalf of the
     * currently logged-in customer.
     *
     * <p>Validations applied here (before reaching the DAO):
     * <ul>
     *   <li>A customer session must be active.</li>
     *   <li>Rating must be 1–5.</li>
     *   <li>Review text (after trim) must not exceed {@value #MAX_REVIEW_LENGTH} characters.</li>
     *   <li>Blank review is normalised to NULL.</li>
     * </ul>
     *
     * <p>The DAO additionally verifies (atomically):
     * <ul>
     *   <li>The order item exists.</li>
     *   <li>The owning order belongs to the logged-in customer.</li>
     *   <li>The order status is {@code "Completed"}.</li>
     * </ul>
     *
     * @param orderItemId  PK of the order_items row to rate
     * @param ratingValue  integer 1–5
     * @param reviewText   optional review; null or blank → stored as NULL
     * @return {@code null} on success, or a human-readable error message on failure
     */
    public static String saveRating(int orderItemId, int ratingValue, String reviewText) {
        // ── 1. Session check ──────────────────────────────────────────────────
        Customer customer = SessionManager.getLoggedInCustomer();
        if (customer == null) {
            return "No customer session is active. Please log in.";
        }

        // ── 2. Rating-value validation ────────────────────────────────────────
        if (ratingValue < 1 || ratingValue > 5) {
            return "Rating must be between 1 and 5 (got " + ratingValue + ").";
        }

        // ── 3. Review-text normalisation and length check ─────────────────────
        String trimmed = (reviewText == null) ? null : reviewText.trim();
        if (trimmed != null && trimmed.isEmpty()) trimmed = null;
        if (trimmed != null && trimmed.length() > MAX_REVIEW_LENGTH) {
            return "Review must not exceed " + MAX_REVIEW_LENGTH + " characters.";
        }

        // ── 4. Delegate to DAO (authorization enforced atomically in transaction)
        try {
            dao.saveRating(orderItemId, customer.getCustomerId(), ratingValue, trimmed);
            return null;  // success
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Could not save rating: " + e.getMessage();
        }
    }

    // ── Admin-facing read-only methods (no session check required) ────────────

    /**
     * Returns all review details for the given food item, ordered most-recent first.
     *
     * @throws IllegalArgumentException if foodId is not positive or the food does not exist
     */
    public static List<FoodReviewDetail> getReviewsForFood(int foodId) {
        if (foodId <= 0)
            throw new IllegalArgumentException("Invalid food ID: " + foodId);
        if (FoodService.getFoodItemById(foodId).isEmpty())
            throw new IllegalArgumentException("Food item not found: ID " + foodId);
        return dao.getReviewsByFoodId(foodId);
    }

    /**
     * Returns a rating summary (average, count) for ALL food items in one query.
     * Foods with no ratings have count == 0 and averageRating == 0.0.
     */
    public static Map<Integer, FoodRatingSummary> getRatingSummaries() {
        return dao.getRatingSummaryForAllFoods();
    }
}
