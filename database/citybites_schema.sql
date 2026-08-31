-- ============================================================
-- City Bites Food Management System - Database Schema
-- MySQL 8.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS citybites
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE citybites;

-- ── Food Categories ──────────────────────────────────────────
-- Must be created before food_items because food_items has a FK to it.
CREATE TABLE IF NOT EXISTS food_categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description   VARCHAR(255) NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Admins ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admins (
    admin_id      INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL
);

-- ── Customers ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    customer_id        INT AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(100) NOT NULL,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    password_hash      VARCHAR(100) NOT NULL,
    email              VARCHAR(150) NULL,
    phone_number       VARCHAR(20)  NULL,
    date_of_birth      DATE         NULL,
    profile_image_path VARCHAR(255) NULL,
    delivery_address   VARCHAR(300) NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Food Items ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS food_items (
    food_id        INT AUTO_INCREMENT PRIMARY KEY,
    food_name      VARCHAR(200)   NOT NULL,
    price          DECIMAL(10,2)  NOT NULL,
    available      TINYINT(1)     NOT NULL DEFAULT 1,
    stock_quantity INT            NOT NULL DEFAULT 0,
    image_path     VARCHAR(500)   DEFAULT NULL,
    category_id    INT            NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_food_items_cat FOREIGN KEY (category_id)
        REFERENCES food_categories(category_id) ON DELETE RESTRICT
);

-- ── Orders ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    order_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id   INT            NOT NULL,
    order_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount  DECIMAL(10,2)  NOT NULL,
    status        ENUM('Pending','Preparing','Ready','Completed','Cancelled')
                  NOT NULL DEFAULT 'Pending',
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE RESTRICT
);

-- ── Order Items ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    item_id    INT AUTO_INCREMENT PRIMARY KEY,
    order_id   INT            NOT NULL,
    food_id    INT            NOT NULL,
    food_name  VARCHAR(200)   NOT NULL,
    unit_price DECIMAL(10,2)  NOT NULL,
    quantity   INT            NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)    ON DELETE CASCADE,
    FOREIGN KEY (food_id)  REFERENCES food_items(food_id) ON DELETE RESTRICT
);

-- ── Schema Migrations ────────────────────────────────────────
-- Tracks one-time data migrations applied by DatabaseInitializer.
CREATE TABLE IF NOT EXISTS schema_migrations (
    migration_key VARCHAR(100) PRIMARY KEY,
    applied_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
