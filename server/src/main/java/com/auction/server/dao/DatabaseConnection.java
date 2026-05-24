package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    private static final String URL = "jdbc:mysql://localhost:3306/auction_system?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "admin";

    private static final String H2_URL = "jdbc:h2:mem:auction_test;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";

    private boolean isTestMode = false;


    private DatabaseConnection() {
        try {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element.getClassName().contains("org.junit") || element.getClassName().contains("surefire")) {
                    isTestMode = true;
                    break;
                }
            }

            if (isTestMode) {
                Class.forName("org.h2.Driver");
                this.connection = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
                logger.info("✅ TEST MODE: Đã kết nối H2 Database ảo trên RAM thành công!");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
                this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
                logger.info("✅ PRODUCTION MODE: Database MySQL connected successfully");
            }

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
                        image_url VARCHAR(500),
                        duration_hours INT DEFAULT 24,
                        start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        end_time TIMESTAMP NULL,
                        status ENUM('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED') DEFAULT 'PENDING',
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);

            try {
                stmt.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'");
            } catch (SQLException e) {
                logger.error("Column status already exists or error: ", e);
            }

            stmt.execute(createProductsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createAutoBidsTable);
            stmt.execute(createUserProductsTable);

            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdmin)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertAdmin = "INSERT INTO users (username, password, email, full_name, role, balance, status) " +
                            "VALUES ('admin', 'admin123', 'admin@auction.com', 'System Admin', 'ADMIN', 0, 'ACTIVE')";
                    stmt.execute(insertAdmin);
                    logger.info("Default admin created: username='admin', password='admin123'");
                }
            }

            logger.info("Database tables created/verified successfully.");
        } catch (SQLException e) {
            logger.error("Error creating tables: ", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || (!isTestMode && !connection.isValid(2))) {
                if (isTestMode) {
                    connection = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
                } else {
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                }
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