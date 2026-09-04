package citybites.model;

import java.time.LocalDateTime;

/**
 * Read-only DTO for admin-facing review display.
 * Contains no password hash, username, or customer_id — only the
 * customer's display name and the rating/review data itself.
 */
public class FoodReviewDetail {

    private int           foodId;
    private String        foodName;
    private String        categoryName;   // nullable
    private String        imagePath;      // nullable — used only via ImageManager.loadScaled
    private String        customerFullName;
    private int           rating;         // 1–5
    private String        reviewText;     // nullable
    private LocalDateTime orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FoodReviewDetail() {}

    public int           getFoodId()           { return foodId; }
    public void          setFoodId(int v)       { foodId = v; }

    public String        getFoodName()          { return foodName; }
    public void          setFoodName(String v)  { foodName = v; }

    public String        getCategoryName()      { return categoryName; }
    public void          setCategoryName(String v) { categoryName = v; }

    public String        getImagePath()         { return imagePath; }
    public void          setImagePath(String v) { imagePath = v; }

    public String        getCustomerFullName()  { return customerFullName; }
    public void          setCustomerFullName(String v) { customerFullName = v; }

    public int           getRating()            { return rating; }
    public void          setRating(int v)       { rating = v; }

    public String        getReviewText()        { return reviewText; }
    public void          setReviewText(String v){ reviewText = v; }

    public LocalDateTime getOrderDate()         { return orderDate; }
    public void          setOrderDate(LocalDateTime v) { orderDate = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { createdAt = v; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
