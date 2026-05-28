package com.auction.server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
  private static volatile DatabaseConnection instance;
  private HikariDataSource dataSource;
  private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

  private static final String URL = "jdbc:mysql://zephyr.proxy.rlwy.net:53289/auction_system";
  private static final String USER = "root";
  private static final String PASSWORD = "ThQyIjNwDIeHMLXpuftiXwjwKxhecSYU";

  private static final String H2_URL = "jdbc:h2:mem:auction_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=TYPE";
  private static final String H2_USER = "sa";
  private static final String H2_PASSWORD = "";

  private boolean isTestMode = false;

  private DatabaseConnection() {
    initDataSource();
    createTables();
  }

  public static DatabaseConnection getInstance() {
    if (instance == null) {
      synchronized (DatabaseConnection.class) {
        if (instance == null) {
          instance = new DatabaseConnection();
        }
      }
    }
    return instance;
  }

  private void initDataSource() {
    // Simple logic to detect if we're running tests
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      if (element.getClassName().contains("org.junit") || element.getClassName().contains("surefire")) {
        isTestMode = true;
        break;
      }
    }

    HikariConfig config = new HikariConfig();
    if (isTestMode) {
      config.setJdbcUrl(H2_URL);
      config.setUsername(H2_USER);
      config.setPassword(H2_PASSWORD);
      config.setDriverClassName("org.h2.Driver");
      config.setMaximumPoolSize(10);
      logger.info("✅ TEST MODE: Đã khởi tạo HikariCP với H2 Database ảo trên RAM thành công!");
    } else {
      config.setJdbcUrl(URL);
      config.setUsername(USER);
      config.setPassword(PASSWORD);
      config.setDriverClassName("com.mysql.cj.jdbc.Driver");

      // Cấu hình tối ưu cho HikariCP
      config.setMaximumPoolSize(20); // Số connection tối đa
      config.setMinimumIdle(5); // Số connection tối thiểu luôn duy trì
      config.setIdleTimeout(300000); // 5 phút (thời gian tối đa connection rảnh rỗi)
      config.setConnectionTimeout(20000); // 20 giây (thời gian chờ tối đa khi lấy connection)
      config.setMaxLifetime(1200000); // 20 phút (thời gian sống tối đa của 1 connection)

      // Cấu hình tối ưu MySQL JDBC Driver
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");

      logger.info("✅ PRODUCTION MODE: Đã khởi tạo HikariCP với MySQL thành công!");
    }

    this.dataSource = new HikariDataSource(config);
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

    String createTransactionsTable = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT,
                    amount DOUBLE,
                    type VARCHAR(50),
                    description VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB
            """;

    // Sử dụng một connection từ pool (qua try-with-resources để đảm bảo đóng tự động)
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
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
      stmt.execute(createTransactionsTable);

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
      // Lấy 1 connection rảnh rỗi từ trong HikariCP Pool
      return dataSource.getConnection();
    } catch (SQLException e) {
      logger.error("Error getting connection from HikariCP pool: ", e);
      return null;
    }
  }

  public void closeConnection() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      logger.info("HikariCP connection pool closed.");
    }
  }
}