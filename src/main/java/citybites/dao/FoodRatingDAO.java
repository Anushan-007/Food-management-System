package citybites.dao;

import citybites.model.FoodRating;
import citybites.model.FoodRatingSummary;
import citybites.model.FoodReviewDetail;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data-access contract for food ratings.
 */
public interface FoodRatingDAO {

    /**
     * Returns the rating for a specific order item, or empty if none exists.
     */
    Optional<FoodRating> findByOrderItemId(int orderItemId);

    /**
     * Returns all ratings for every item in the given order, in item order.
     * Populates the display-only {@code foodName} and {@code orderId} fields.
     */
    List<FoodRating> findByOrderId(int orderId);

    /**
     * Saves (inserts or updates) a rating for {@code orderItemId}.
     *
     * <p>This method enforces all authorization rules atomically inside a single
     * JDBC transaction:
     * <ul>
     *   <li>The order item must exist.</li>
     *   <li>The owning order must belong to {@code customerId}.</li>
     *   <li>The order status must be {@code "Completed"}.</li>
     *   <li>If a rating already exists it is updated; otherwise a new one is inserted.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if authorization fails or the item is not found
     * @throws RuntimeException         on an unexpected database error
     */
    void saveRating(int orderItemId, int customerId, int ratingValue, String reviewText);

    /**
     * Returns full review details for every rating of the given food item,
     * ordered by rating creation date descending (most recent first).
     * Returns an empty list when the food has no ratings.
     */
    List<FoodReviewDetail> getReviewsByFoodId(int foodId);

    /**
     * Returns a rating summary (average, count) for ALL food items in a single query.
     * Foods with no ratings are included with count=0 and averageRating=0.0.
     * Using a single grouped query prevents N+1 loading when displaying the food table.
     */
    Map<Integer, FoodRatingSummary> getRatingSummaryForAllFoods();
}
