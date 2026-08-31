package citybites.service;

import citybites.dao.FoodItemDAO;
import citybites.dao.impl.FoodItemDAOImpl;
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
}
