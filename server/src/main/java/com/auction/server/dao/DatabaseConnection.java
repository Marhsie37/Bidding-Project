package com.auction.server.dao;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String URL = "jdbc:mysql://localhost:3306/auction_system?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.info("Database connected successfully");

            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Database connection error: ", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void createTables() {
        String createUsersTable = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) UNIQUE NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(100) UNIQUE NOT NULL,
                        full_name VARCHAR(100),
                        role ENUM('BIDDER', 'SELLER', 'ADMIN') DEFAULT 'BIDDER',
                        balance DECIMAL(15,2) DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        active BOOLEAN DEFAULT TRUE,
                        status VARCHAR(20) DEFAULT 'ACTIVE'
                    ) ENGINE=InnoDB
                """;

        String createProductsTable = """
                    CREATE TABLE IF NOT EXISTS products (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(200) NOT NULL,
                        description TEXT,
                        starting_price DECIMAL(15,2) NOT NULL,
                        current_price DECIMAL(15,2),
                        seller_id INT NOT NULL,
                        category VARCHAR(50),
                        image_url LONGTEXT,
                        duration_hours INT DEFAULT 24,
                        start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        end_time TIMESTAMP NULL,
                        status ENUM('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED', 'SOLD') DEFAULT 'PENDING',
                        winner_id INT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_product_winner FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
                    ) ENGINE=InnoDB
                """;

        String createBidsTable = """
                    CREATE TABLE IF NOT EXISTS bids (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        product_id INT NOT NULL,
                        bidder_id INT NOT NULL,
                        bid_amount DECIMAL(15,2) NOT NULL,
                        bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        is_auto_bid BOOLEAN DEFAULT FALSE,
                        CONSTRAINT fk_bid_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                        CONSTRAINT fk_bid_user FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                """;

        String createAutoBidsTable = """
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
                    ) ENGINE=InnoDB
                """;

        String createUserProductsTable = """
                    CREATE TABLE IF NOT EXISTS user_products (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        product_id INT NOT NULL,
                        product_name VARCHAR(200) NOT NULL,
                        product_price DECIMAL(15,2) NOT NULL,
                        purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_up_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                        UNIQUE KEY unique_user_product (user_id, product_id)
                    ) ENGINE=InnoDB
                """;
        // Tạo bảng user_product là sản phẩm mà người thắng nhận được

        try (Statement stmt = connection.createStatement()) {
            // Tạo bảng users (đã có cột status)
            stmt.execute(createUsersTable);

            // Kiểm tra nếu cột status chưa có thì thêm (cho database cũ)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'");
            } catch (SQLException e) {
                // Cột có thể đã tồn tại, bỏ qua lỗi
                logger.error("Column status already exists or error: ", e);
            }

            stmt.execute(createProductsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createAutoBidsTable);
            stmt.execute(createUserProductsTable);

            try {
                stmt.execute("ALTER TABLE products MODIFY COLUMN image_url LONGTEXT");
            } catch (SQLException e) {
                logger.warn("Could not modify image_url column (may already be LONGTEXT): " + e.getMessage());
            }

            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdmin)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertAdmin = "INSERT INTO users (username, password, email, full_name, role, balance, status) "
                            +
                            "VALUES ('admin', 'admin123', 'admin@auction.com', 'System Admin', 'ADMIN', 0, 'ACTIVE')";
                    stmt.execute(insertAdmin);
                    logger.info("Default admin created: username='admin', password='admin123'");
                }
            }

            logger.info("Database tables created/verified successfully.");
        } catch (SQLException e) {
            logger.error("Error creating tables: ", e);
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                logger.info("🔄 Đã kết nối lại database");
            }
        } catch (SQLException e) {
            logger.error("Error reconnecting: ", e);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Error closing connection: ", e);
        }
    }

}