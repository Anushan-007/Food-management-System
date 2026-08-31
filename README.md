# CityBites Food Management System

A Java Swing desktop application backed by MySQL for managing a food-ordering operation.

---

## Default Credentials

| Role     | Username   | Password   | Source                                   |
|----------|------------|------------|------------------------------------------|
| Admin    | `admin`    | `admin123` | `DatabaseInitializer.seedAdmin()`        |
| Customer | `customer` | `Demo1234` | `DatabaseInitializer.seedCustomer()`     |

> **Important — live DB state**: `seedAdmin()` and `seedCustomer()` only insert if the
> row does not already exist. If a password was changed in an existing database, the seed
> will **not** overwrite it. Run the recovery procedure below if a demo account no longer
> accepts its intended credential.

---

## Seeded-Account Password Recovery

If `AuthService.customerLogin("customer", "Demo1234")` returns `Optional.empty()`, the
`password_hash` stored in MySQL was changed after the initial seed. To reset it:

1. Generate a fresh BCrypt hash in any Java snippet:
   ```java
   System.out.println(BCrypt.hashpw("Demo1234", BCrypt.gensalt()));
   // prints e.g.  $2a$10$abc...xyz  (60 chars, changes each run due to random salt)
   ```

2. Run the UPDATE in a MySQL shell:
   ```sql
   UPDATE customers
   SET    password_hash = '$2a$10$<paste_printed_hash_here>'
   WHERE  username = 'customer';
   ```

3. Restart the application. `seedCustomer()` will skip (row exists) and the new hash is live.

For the admin account the same approach applies using `"admin123"` and the `admins` table.

---

## Technology Stack

- Java 21+ (tested with NetBeans-bundled JDK 26)
- Java Swing (desktop UI)
- MySQL 8.0
- Maven 3.9
- JUnit 5
- jBCrypt (`org.mindrot.jbcrypt`)
