/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.data;
import citybites.model.Customer;
import citybites.model.FoodItem;
import java.util.ArrayList;
import citybites.model.CartItem;
import citybites.model.Order;
/**
 *
 * @author User
 */
public class DataStore {
    
    public static final ArrayList<FoodItem> foodItems =
            new ArrayList<>();

    public static final ArrayList<Customer> customers =
            new ArrayList<>();
    
    public static final ArrayList<CartItem> cartItems =
        new ArrayList<>();
    
    public static final ArrayList<Order> orders =
        new ArrayList<>();

    private static int nextFoodId = 1;
    private static int nextCustomerId = 1;
    private static int nextOrderId = 1;

    private DataStore() {
    }

    public static int generateFoodId() {
        return nextFoodId++;
    }

    public static int generateCustomerId() {
        return nextCustomerId++;
    }
    
    public static int generateOrderId() {
    return nextOrderId++;
}

    public static void loadSampleData() {
        if (!foodItems.isEmpty()) {
            return;
        }
        

        foodItems.add(new FoodItem(
                generateFoodId(),
                "Chicken Fried Rice",
                850.00,
                true
        ));

        foodItems.add(new FoodItem(
                generateFoodId(),
                "Chicken Kottu",
                750.00,
                true
        ));

        foodItems.add(new FoodItem(
                generateFoodId(),
                "Cheese Burger",
                650.00,
                true
        ));

        customers.add(new Customer(
                generateCustomerId(),
                "Sample Customer",
                "customer",
                "1234"
        ));
    }
    
}
