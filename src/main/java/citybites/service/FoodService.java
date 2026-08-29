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

    /** Returns all food items (for admin management view). */
    public static List<FoodItem> getAllFoodItems() {
        return foodItemDAO.findAll();
    }

    /** Returns only available food items (for customer menu view). */
    public static List<FoodItem> getAvailableFoodItems() {
        return foodItemDAO.findAvailable();
    }

    /** Looks up a food item by its ID. */
    public static Optional<FoodItem> getFoodItemById(int foodId) {
        return foodItemDAO.findById(foodId);
    }

    /**
     * Adds a new food item.
     * @param name      display name
     * @param price     price in Rs.
     * @param available whether it is currently offered
     * @param imagePath optional file path for an image (may be null)
     * @return the generated food ID
     */
    public static int addFoodItem(String name, double price, boolean available, String imagePath) {
        FoodItem item = new FoodItem();
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setImagePath(imagePath);
        return foodItemDAO.insert(item);
    }

    /**
     * Updates an existing food item's details.
     * @return true if the record was updated successfully
     */
    public static boolean updateFoodItem(int foodId, String name, double price,
                                         boolean available, String imagePath) {
        FoodItem item = new FoodItem();
        item.setFoodId(foodId);
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setImagePath(imagePath);
        return foodItemDAO.update(item);
    }

    /**
     * Deletes a food item by ID.
     * @return true on success
     * @throws RuntimeException if the item is referenced by existing orders
     */
    public static boolean deleteFoodItem(int foodId) {
        return foodItemDAO.delete(foodId);
    }
}
