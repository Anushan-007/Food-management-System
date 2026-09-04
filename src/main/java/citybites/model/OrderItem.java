/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.model;

/**
 *
 * @author User
 */
public class OrderItem {

    /** order_items.item_id (PK). 0 for cart/new items that have not yet been persisted. */
    private int itemId;
    private int foodId;
    private String foodName;
    private double unitPrice;
    private int quantity;

    /** Existing constructor — itemId defaults to 0 (used for new/cart items). */
    public OrderItem(
            int foodId,
            String foodName,
            double unitPrice,
            int quantity) {

        this.foodId = foodId;
        this.foodName = foodName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public int getItemId()            { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getFoodId() {
        return foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

}
