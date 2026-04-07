package com.auction.server.dao;

import java.sql.*;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/auction_project?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "password"; // Thay bằng mật khẩu của bạn

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Kết nối Database thành công!");
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Lỗi kết nối Database: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void createTables() {
        // 1. Bảng Users
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                full_name VARCHAR(100),
                role ENUM('BIDDER', 'SELLER', 'ADMIN') NOT NULL,
                balance DECIMAL(15,2) DEFAULT 0,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createProductsTable = """
            CREATE TABLE IF NOT EXISTS products (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                starting_price DECIMAL(15,2) NOT NULL,
                current_price DECIMAL(15,2),
                seller_id INT NOT NULL,
                category ENUM('ELECTRONICS', 'ART') NOT NULL,
                image_url VARCHAR(500),
                duration_hours INT DEFAULT 24,
                start_time DATETIME,
                end_time DATETIME,
                status ENUM('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED') DEFAULT 'PENDING',
                winner_id INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL
            )
        """;

        String createBidsTable = """
            CREATE TABLE IF NOT EXISTS bids (
                bid_id INT PRIMARY KEY AUTO_INCREMENT,
                product_id INT NOT NULL,
                bidder_id INT NOT NULL,
                bid_amount DECIMAL(15,2) NOT NULL,
                bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_auto_bid BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                FOREIGN KEY (bidder_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
        """;

        String createAutoBidsTable = """
            CREATE TABLE IF NOT EXISTS auto_bids (
                id INT PRIMARY KEY AUTO_INCREMENT,
                product_id INT NOT NULL,
                user_id INT NOT NULL,
                max_bid DECIMAL(15,2) NOT NULL,
                increment_amount DECIMAL(15,2) DEFAULT 1000,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                UNIQUE KEY unique_auto_bid (product_id, user_id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createProductsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createAutoBidsTable);

            // Tạo Admin mặc định
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            ResultSet rs = stmt.executeQuery(checkAdmin);
            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdmin = "INSERT INTO users (username, password_hash, email, full_name, role, balance) " +
                        "VALUES ('admin', 'admin123', 'admin@auction.com', 'System Admin', 'ADMIN', 0)";
                stmt.execute(insertAdmin);
            }
            System.out.println("Hệ thống bảng đã sẵn sàng.");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo bảng: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối lại: " + e.getMessage());
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Đã đóng kết nối Database.");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}