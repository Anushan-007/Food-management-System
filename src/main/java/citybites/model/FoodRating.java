package citybites.model;

import java.time.LocalDateTime;

/**
 * Represents a customer's rating and optional review for a specific order item.
 * Ownership and food identity are resolved through order_item → order and
 * order_item → food_items relations; no redundant customer_id or food_id stored here.
 */
public class FoodRating {

    private int ratingId;
    private int orderItemId;
    private int rating;           // 1–5 inclusive
    private String reviewText;    // nullable; blank is stored as NULL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Display-only fields populated by DAO queries (not columns in food_ratings)
    private String foodName;
    private int orderId;

    public FoodRating() {}

    public int getRatingId()                   { return ratingId; }
    public void setRatingId(int ratingId)      { this.ratingId = ratingId; }

    public int getOrderItemId()                { return orderItemId; }
    public void setOrderItemId(int orderItemId){ this.orderItemId = orderItemId; }

    public int getRating()                     { return rating; }
    public void setRating(int rating)          { this.rating = rating; }

    public String getReviewText()              { return reviewText; }
    public void setReviewText(String t)        { reviewText = t; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { createdAt = t; }

    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { updatedAt = t; }

    public String getFoodName()                { return foodName; }
    public void setFoodName(String name)       { foodName = name; }

    public int getOrderId()                    { return orderId; }
    public void setOrderId(int orderId)        { this.orderId = orderId; }
}
