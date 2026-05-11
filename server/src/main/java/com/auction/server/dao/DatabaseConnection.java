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
    private static final String PASSWORD = "admin";

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.info("Database connected successfully");

            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Database connection error: ",e);
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
                -- THÊM DÒNG DƯỚI ĐÂY --
                balance DECIMAL(15,2) DEFAULT 0.00, 
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                active BOOLEAN DEFAULT TRUE
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createProductsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createAutoBidsTable);

            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdmin)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertAdmin = "INSERT INTO users (username, password, email, full_name, role, balance) " +
                            "VALUES ('admin', 'admin123', 'admin@auction.com', 'System Admin', 'ADMIN', 0)";
                    stmt.execute(insertAdmin);
                    logger.info("Default admin created: username='admin', password='admin123'");
                }
            }

            logger.info("Database tables created/verified successfully.");
        } catch (SQLException e) {
            logger.error("Error creating tables: ",e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            logger.error("Error reconnecting: ",e);
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
            logger.error("Error closing connection: ",e);
        }
    }
}