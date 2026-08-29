/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package citybites.util;
import citybites.model.Customer;

/**
 *
 * @author User
 */
public class SessionManager {
    
    
    private static Customer loggedInCustomer;

    private SessionManager() {
    }

    public static Customer getLoggedInCustomer() {
        return loggedInCustomer;
    }

    public static void setLoggedInCustomer(Customer customer) {
        loggedInCustomer = customer;
    }

    public static void logout() {
        loggedInCustomer = null;
    }
    
    
}
