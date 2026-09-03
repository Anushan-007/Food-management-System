package citybites.service;

import citybites.dao.FoodItemDAO;
import citybites.dao.impl.FoodItemDAOImpl;
import citybites.model.FeaturedAssignmentResult;
import citybites.model.FoodItem;
import java.util.List;
import java.util.Optional;

/**
 * Handles CRUD operations for food items.
 */
public class FoodService {

    private static final FoodItemDAO foodItemDAO = new FoodItemDAOImpl();

    private FoodService() {}

    public static List<FoodItem> getAllFoodItems() {
        return foodItemDAO.findAll();
    }

    public static List<FoodItem> getAvailableFoodItems() {
        return foodItemDAO.findAvailable();
    }

    public static Optional<FoodItem> getFoodItemById(int foodId) {
        return foodItemDAO.findById(foodId);
    }

    /**
     * Adds a food item, automatically assigning it to the "Other" category.
     * Retained for backward compatibility — all new callers should supply a category.
     */
    public static int addFoodItem(String name, double price, boolean available,
                                   int stockQuantity, String imagePath) {
        return addFoodItem(name, price, available, stockQuantity, imagePath,
                           FoodCategoryService.getOtherCategoryId());
    }

    /**
     * Adds a food item assigned to the specified category.
     *
     * @throws IllegalArgumentException if {@code categoryId} is not positive, or if no
     *                                  category with that ID exists in food_categories
     */
    public static int addFoodItem(String name, double price, boolean available,
                                   int stockQuantity, String imagePath, int categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("A valid category is required for food items.");
        }
        if (!FoodCategoryService.categoryExistsById(categoryId)) {
            throw new IllegalArgumentException("Selected category does not exist.");
        }
        FoodItem item = new FoodItem();
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
        item.setCategoryId(categoryId);
        return foodItemDAO.insert(item);
    }

    /**
     * Updates a food item, retaining its category assignment via the "Other" fallback.
     * Retained for backward compatibility — all new callers should supply a category.
     */
    public static boolean updateFoodItem(int foodId, String name, double price,
                                          boolean available, int stockQuantity, String imagePath) {
        return updateFoodItem(foodId, name, price, available, stockQuantity, imagePath,
                              FoodCategoryService.getOtherCategoryId());
    }

    /**
     * Updates a food item including its category assignment.
     *
     * @throws IllegalArgumentException if {@code categoryId} is not positive, or if no
     *                                  category with that ID exists in food_categories
     */
    public static boolean updateFoodItem(int foodId, String name, double price,
                                          boolean available, int stockQuantity,
                                          String imagePath, int categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("A valid category is required for food items.");
        }
        if (!FoodCategoryService.categoryExistsById(categoryId)) {
            throw new IllegalArgumentException("Selected category does not exist.");
        }
        FoodItem item = new FoodItem();
        item.setFoodId(foodId);
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
        item.setCategoryId(categoryId);
        return foodItemDAO.update(item);
    }

    public static boolean deleteFoodItem(int foodId) {
        return foodItemDAO.delete(foodId);
    }

    public static int countAllFoodItems() {
        return foodItemDAO.countAll();
    }

    public static int countAvailableFoodItems() {
        return foodItemDAO.countAvailable();
    }

    // ── Featured-foods (dashboard) ───────────────────────────────────────────

    /** Returns foods configured for the customer dashboard, ordered by slot (1→4). */
    public static List<FoodItem> getFeaturedFoodItems() {
        return foodItemDAO.findFeatured();
    }

    /**
     * Returns the food_id of whatever food currently occupies {@code slot} (excluding
     * {@code excludeFoodId}), or {@code 0} when the slot is free.
     *
     * <p>This is a plain read — no locks, no transaction.  Use it as a pre-flight check
     * in the UI so a confirmation dialog can be shown <em>before</em> any mutation.
     */
    public static int findFeaturedSlotOccupant(int slot, int excludeFoodId) {
        return foodItemDAO.findSlotOccupant(slot, excludeFoodId);
    }

    /**
     * Assigns a dashboard slot (1–4) to a food item, or clears it (null).
     * Always displaces any food that currently holds the target slot.
     * Convenience wrapper over {@link #assignFeaturedPosition} with {@code replaceOccupied=true}.
     *
     * @throws IllegalArgumentException if position is non-null and outside range 1–4
     */
    public static boolean setFeaturedPosition(int foodId, Integer position) {
        if (position != null && (position < 1 || position > 4)) {
            throw new IllegalArgumentException(
                "Featured slot must be between 1 and 4 (got " + position + ").");
        }
        return foodItemDAO.assignFeaturedPosition(foodId, position, true).isSuccess();
    }

    /**
     * Transactional slot assignment exposed to the UI layer.
     *
     * <p>When {@code replaceOccupied} is {@code false} and the target slot is already
     * taken, returns {@link FeaturedAssignmentResult.Status#SLOT_OCCUPIED} with no DB
     * change, allowing the caller to show a confirmation dialog before retrying with
     * {@code replaceOccupied=true}.
     *
     * @throws IllegalArgumentException if targetPosition is non-null and outside range 1–4
     */
    public static FeaturedAssignmentResult assignFeaturedPosition(
            int foodId, Integer targetPosition, boolean replaceOccupied) {
        if (targetPosition != null && (targetPosition < 1 || targetPosition > 4)) {
            throw new IllegalArgumentException(
                "Featured slot must be between 1 and 4 (got " + targetPosition + ").");
        }
        return foodItemDAO.assignFeaturedPosition(foodId, targetPosition, replaceOccupied);
    }

    /**
     * Atomically updates a food item's fields and its featured-slot assignment in a
     * single JDBC transaction.
     *
     * <p>Returns {@link FeaturedAssignmentResult.Status#SLOT_OCCUPIED} (with no DB change)
     * when {@code targetPosition} is already taken and {@code replaceOccupied} is
     * {@code false}, allowing the caller to prompt the user before retrying with
     * {@code replaceOccupied=true}.
     *
     * @throws IllegalArgumentException if {@code categoryId} is invalid or does not exist,
     *                                  or if {@code targetPosition} is outside 1–4
     * @throws RuntimeException wrapping any SQL error (transaction rolled back)
     */
    public static FeaturedAssignmentResult updateFoodItemWithFeaturedPosition(
            int foodId, String name, double price, boolean available,
            int stockQuantity, String imagePath, int categoryId,
            Integer targetPosition, boolean replaceOccupied) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("A valid category is required for food items.");
        }
        if (!FoodCategoryService.categoryExistsById(categoryId)) {
            throw new IllegalArgumentException("Selected category does not exist.");
        }
        if (targetPosition != null && (targetPosition < 1 || targetPosition > 4)) {
            throw new IllegalArgumentException(
                "Featured slot must be between 1 and 4 (got " + targetPosition + ").");
        }
        FoodItem item = new FoodItem();
        item.setFoodId(foodId);
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
        item.setCategoryId(categoryId);
        return foodItemDAO.updateWithFeaturedPosition(item, targetPosition, replaceOccupied);
    }

    /**
     * Atomically inserts a new food item and optionally assigns it to a dashboard slot,
     * all within a single JDBC transaction.
     *
     * <p>The occupancy check happens <em>before</em> the INSERT, so if the requested
     * slot is taken and {@code replaceOccupied} is {@code false}, no database change
     * of any kind is made (no orphan food row is left behind).
     *
     * <p>Typical two-phase UI pattern:
     * <pre>
     *   int id = addFoodItemWithFeaturedPosition(..., slot, false);
     *   if (id == 0) {  // SLOT_OCCUPIED — nothing inserted yet
     *       if (userConfirms()) {
     *           id = addFoodItemWithFeaturedPosition(..., slot, true);
     *       } else {
     *           deleteImportedImage();  // only side-effect to undo
     *           return;
     *       }
     *   }
     * </pre>
     *
     * @return new food_id on success; {@code 0} when {@code featuredPosition} is non-null,
     *         the slot is occupied, and {@code replaceOccupied} is {@code false}
     * @throws IllegalArgumentException if {@code categoryId} is invalid or does not exist,
     *                                  or if {@code featuredPosition} is outside 1–4
     * @throws RuntimeException wrapping any SQL error (transaction rolled back)
     */
    public static int addFoodItemWithFeaturedPosition(
            String name, double price, boolean available, int stockQuantity,
            String imagePath, int categoryId,
            Integer featuredPosition, boolean replaceOccupied) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("A valid category is required for food items.");
        }
        if (!FoodCategoryService.categoryExistsById(categoryId)) {
            throw new IllegalArgumentException("Selected category does not exist.");
        }
        if (featuredPosition != null && (featuredPosition < 1 || featuredPosition > 4)) {
            throw new IllegalArgumentException(
                "Featured slot must be between 1 and 4 (got " + featuredPosition + ").");
        }
        FoodItem item = new FoodItem();
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
        item.setCategoryId(categoryId);
        return foodItemDAO.insertWithFeaturedPosition(item, featuredPosition, replaceOccupied);
    }
}
