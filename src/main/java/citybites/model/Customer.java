/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.model;

/**
 *
 * @author User
 */
public class Customer {
    
    private int customerId;
    private String fullName;
    private String username;
    private String password;

    public Customer(int customerId, String fullName,
                    String username, String password) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
