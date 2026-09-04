package citybites.model;

/**
 * Read-only DTO carrying the aggregate rating data for a single food item.
 * Produced by a single grouped SQL query to prevent N+1 loading.
 */
public class FoodRatingSummary {

    private final int    foodId;
    private final double averageRating;  // 0.0 when ratingCount == 0
    private final int    ratingCount;

    public FoodRatingSummary(int foodId, double averageRating, int ratingCount) {
        this.foodId        = foodId;
        this.averageRating = averageRating;
        this.ratingCount   = ratingCount;
    }

    public int    getFoodId()        { return foodId; }
    public double getAverageRating() { return averageRating; }
    public int    getRatingCount()   { return ratingCount; }

    /** "4.5 / 5" or "—" when no ratings exist. */
    public String formatAverage() {
        return ratingCount > 0 ? String.format("%.1f / 5", averageRating) : "\u2014";
    }

    /** "12 reviews" | "1 review" | "0 reviews". */
    public String formatCount() {
        return ratingCount == 1 ? "1 review" : ratingCount + " reviews";
    }
}
