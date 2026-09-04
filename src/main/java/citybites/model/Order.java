/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.model;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 *
 * @author User
 */
public class Order {

    // ── Canonical status constants ────────────────────────────────────────────
    public static final String STATUS_PENDING   = "Pending";
    public static final String STATUS_PREPARING = "Preparing";
    public static final String STATUS_READY     = "Ready";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancelled";

    private int orderId;
    private Customer customer;
    private LocalDateTime orderDate;
    private ArrayList<OrderItem> orderItems;
    private double totalAmount;
    private String status;

    public Order(
            int orderId,
            Customer customer,
            LocalDateTime orderDate,
            ArrayList<OrderItem> orderItems,
            double totalAmount,
            String status) {

        this.orderId = orderId;
        this.customer = customer;
        this.orderDate = orderDate;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public ArrayList<OrderItem> getOrderItems() {
        return orderItems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
