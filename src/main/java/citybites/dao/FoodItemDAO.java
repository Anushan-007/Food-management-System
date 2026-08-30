package citybites.dao;

import citybites.model.FoodItem;
import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for the food_items table.
 */
public interface FoodItemDAO {

    List<FoodItem> findAll();

    List<FoodItem> findAvailable();

    Optional<FoodItem> findById(int foodId);

    int insert(FoodItem item);

    boolean update(FoodItem item);

    boolean delete(int foodId);

    int countAll();

    int countAvailable();
}
