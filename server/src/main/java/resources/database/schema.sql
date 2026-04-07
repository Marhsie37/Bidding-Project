USE auction_system;

-- 1. Table Users
CREATE TABLE IF NOT EXISTS users (
                                     id INT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    role ENUM('BIDDER', 'SELLER', 'ADMIN') DEFAULT 'BIDDER',
    balance DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
    ) ENGINE=InnoDB;

-- 2. Table Products
CREATE TABLE IF NOT EXISTS products (
                                        id INT PRIMARY KEY AUTO_INCREMENT,
                                        name VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    starting_price DECIMAL(15,2) NOT NULL,
    current_price DECIMAL(15,2),
    seller_id INT NOT NULL,
    category VARCHAR(50),
    image_url VARCHAR(500),
    duration_hours INT DEFAULT 24,
    start_time TIMESTAMP NULL DEFAULT NULL,
    end_time TIMESTAMP NULL DEFAULT NULL,
    status ENUM('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED') DEFAULT 'PENDING',
    winner_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_winner FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB;

-- 3. Table Bids
CREATE TABLE IF NOT EXISTS bids (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    product_id INT NOT NULL,
                                    bidder_id INT NOT NULL,
                                    bid_amount DECIMAL(15,2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_bid_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_user FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB;

-- 4. Table Auto Bids
CREATE TABLE IF NOT EXISTS auto_bids (
                                         id INT PRIMARY KEY AUTO_INCREMENT,
                                         product_id INT NOT NULL,
                                         user_id INT NOT NULL,
                                         max_bid DECIMAL(15,2) NOT NULL,
    increment_amount DECIMAL(15,2) DEFAULT 1000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_auto_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_auto_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_auto_bid (product_id, user_id)
    ) ENGINE=InnoDB;

-- 5. Insert dữ liệu Admin mặc định
INSERT IGNORE INTO users (username, password, email, full_name, role, balance)
VALUES ('admin', 'admin123', 'admin@auction.com', 'System Admin', 'ADMIN', 0);