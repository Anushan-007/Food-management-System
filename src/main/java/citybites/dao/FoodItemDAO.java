package citybites.dao;

import citybites.model.FoodItem;
import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for the food_items table.
 */
public interface FoodItemDAO {

    /** Returns every food item (available and unavailable). */
    List<FoodItem> findAll();

    /** Returns only food items where available = true. */
    List<FoodItem> findAvailable();

    /** Returns a single food item by primary key. */
    Optional<FoodItem> findById(int foodId);

    /** Inserts a new food item; returns the auto-generated ID. */
    int insert(FoodItem item);

    /** Updates an existing food item. Returns true on success. */
    boolean update(FoodItem item);

    /** Deletes a food item by ID. Returns true on success. */
    boolean delete(int foodId);
}
