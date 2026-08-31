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

    private int    customerId;
    private String fullName;
    private String username;
    private String password;
    private String createdAt;

    // ── Optional profile fields (all nullable) ────────────────────────────────
    private String              email;
    private String              phoneNumber;
    private java.time.LocalDate dateOfBirth;
    private String              profileImagePath;
    private String              deliveryAddress;

    /** Used by AuthService / CustomerDAOImpl.findByUsername — createdAt not needed there. */
    public Customer(int customerId, String fullName,
                    String username, String password) {
        this.customerId = customerId;
        this.fullName   = fullName;
        this.username   = username;
        this.password   = password;
    }

    /** Used by CustomerManagementService / getAll — includes registration timestamp. */
    public Customer(int customerId, String fullName,
                    String username, String password, String createdAt) {
        this.customerId = customerId;
        this.fullName   = fullName;
        this.username   = username;
        this.password   = password;
        this.createdAt  = createdAt;
    }

    /** Used by CustomerProfileService — includes all profile fields. */
    public Customer(int customerId, String fullName, String username, String password,
                    String createdAt, String email, String phoneNumber,
                    java.time.LocalDate dateOfBirth, String profileImagePath,
                    String deliveryAddress) {
        this(customerId, fullName, username, password, createdAt);
        this.email            = email;
        this.phoneNumber      = phoneNumber;
        this.dateOfBirth      = dateOfBirth;
        this.profileImagePath = profileImagePath;
        this.deliveryAddress  = deliveryAddress;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int    getCustomerId()      { return customerId; }
    public String getFullName()        { return fullName; }
    public String getUsername()        { return username; }
    public String getPassword()        { return password; }
    public String getCreatedAt()       { return createdAt; }
    public String getEmail()           { return email; }
    public String getPhoneNumber()     { return phoneNumber; }
    public java.time.LocalDate getDateOfBirth()    { return dateOfBirth; }
    public String getProfileImagePath() { return profileImagePath; }
    public String getDeliveryAddress() { return deliveryAddress; }

    // ── Profile setters ───────────────────────────────────────────────────────

    public void setFullName(String fullName)               { this.fullName        = fullName; }
    public void setEmail(String email)                     { this.email           = email; }
    public void setPhoneNumber(String phoneNumber)         { this.phoneNumber     = phoneNumber; }
    public void setDateOfBirth(java.time.LocalDate dob)    { this.dateOfBirth     = dob; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
