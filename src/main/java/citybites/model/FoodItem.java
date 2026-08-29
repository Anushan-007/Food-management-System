/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.model;

/**
 *
 * @author User
 */
public class FoodItem {
    
    private int foodId;
    private String foodName;
    private double price;
    private boolean available;

    public FoodItem() {
    }

    public FoodItem(int foodId, String foodName,
                    double price, boolean available) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.price = price;
        this.available = available;
    }

    public int getFoodId() {
        return foodId;
    }

    public void setFoodId(int foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
    
}
