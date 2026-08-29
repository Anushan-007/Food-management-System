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

    public static int addFoodItem(String name, double price, boolean available,
                                   int stockQuantity, String imagePath) {
        FoodItem item = new FoodItem();
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
        return foodItemDAO.insert(item);
    }

    public static boolean updateFoodItem(int foodId, String name, double price,
                                          boolean available, int stockQuantity, String imagePath) {
        FoodItem item = new FoodItem();
        item.setFoodId(foodId);
        item.setFoodName(name);
        item.setPrice(price);
        item.setAvailable(available);
        item.setStockQuantity(stockQuantity);
        item.setImagePath(imagePath);
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
