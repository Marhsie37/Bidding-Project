package com.auction.server.dao;

import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BidDAOTest {
  private Connection testConn;
  private BidDAO bidDAO;
  private UserDAO userDAO;
  private ProductDAO productDAO;
  private static final Logger logger = LoggerFactory.getLogger(BidDAOTest.class);
  private int testUserId;
  private int testProductId;

  @BeforeAll
  void setup() throws Exception {
    testConn = DriverManager.getConnection("jdbc:h2:mem:testdb_bid;DB_CLOSE_DELAY=-1");
    try (Statement stmt = testConn.createStatement()) {
      stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
              "id INT PRIMARY KEY AUTO_INCREMENT, " +
              "username VARCHAR(50) UNIQUE, " +
              "password VARCHAR(255), " +
              "email VARCHAR(100) UNIQUE, " +
              "full_name VARCHAR(100), " +
              "role VARCHAR(20), " +
              "balance DOUBLE DEFAULT 0, " +
              "active BOOLEAN DEFAULT TRUE, " +
              "status VARCHAR(20) DEFAULT 'ACTIVE', " +
              "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

      stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
              "id INT PRIMARY KEY AUTO_INCREMENT, " +
              "name VARCHAR(200), " +
              "description TEXT, " +
              "starting_price DOUBLE, " +
              "current_price DOUBLE, " +
              "seller_id INT, " +
              "category VARCHAR(100), " +
              "image_url VARCHAR(255), " +
              "duration_hours INT, " +
              "start_time TIMESTAMP, " +
              "end_time TIMESTAMP, " +
              "status VARCHAR(50), " +
              "winner_id INT, " +
              "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

      stmt.execute("CREATE TABLE IF NOT EXISTS bids (" +
              "id INT PRIMARY KEY AUTO_INCREMENT, " +
              "product_id INT, " +
              "bidder_id INT, " +
              "bid_amount DOUBLE, " +
              "is_auto_bid BOOLEAN DEFAULT FALSE, " +
              "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    bidDAO = new BidDAO(testConn);
    userDAO = new UserDAO(testConn);
    productDAO = new ProductDAO(testConn);

    String uniqueKey = String.valueOf(System.currentTimeMillis());
    String testEmail = "bid_" + uniqueKey + "@test.com";
    String testUsername = "bidder_" + uniqueKey;

    userDAO.createUser(testUsername, "123", testEmail, "Bidder Test", "BIDDER");
    testUserId = userDAO.findByUsername(testUsername).getId();

    Product p = new Product();
    p.setName("Sản phẩm Test " + uniqueKey);
    p.setStartingPrice(1000.0);
    p.setSellerId(1);
    p.setCategory("Test");
    p.setDurationHours(1);
    p.setEndTime(LocalDateTime.now().plusHours(1));

    productDAO.createProduct(p);
    testProductId = p.getId();
  }

  @AfterAll
  void tearDown() throws Exception {
    if (testConn != null) {
      testConn.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test tạo lượt đặt giá mới")
  void testCreateBid() {
    BidTransaction bid = new BidTransaction();
    bid.setAuctionId(testProductId);
    bid.setBidderId(testUserId);
    bid.setBidAmount(1500.0);
    bid.setAutoBid(false);

    boolean result = bidDAO.createBid(bid);

    assertTrue(result, "Phải tạo được lượt đặt giá thành công");
    assertTrue(bid.getId() > 0, "ID của bid phải được tự động sinh ra");
  }

  @Test
  @Order(2)
  @DisplayName("Test lấy giá cao nhất hiện tại")
  void testGetCurrentHighestBid() {
    BidTransaction higherBid = new BidTransaction();
    higherBid.setAuctionId(testProductId);
    higherBid.setBidderId(testUserId);
    higherBid.setBidAmount(2000.0);
    bidDAO.createBid(higherBid);

    double highest = bidDAO.getCurrentHighestBid(testProductId);
    assertEquals(2000.0, highest, "Giá cao nhất phải là 2000.0");
  }

  @Test
  @Order(3)
  @DisplayName("Test lấy người đặt giá cao nhất")
  void testGetCurrentHighestBidder() {
    int bidderId = bidDAO.getCurrentHighestBidder(testProductId);
    assertEquals(testUserId, bidderId, "ID người đặt cao nhất phải khớp");
  }

  @Test
  @Order(4)
  @DisplayName("Test lấy danh sách bid theo sản phẩm")
  void testGetBidsByProduct() {
    List<BidTransaction> bids = bidDAO.getBidsByProduct(testProductId);
    assertNotNull(bids);
    assertTrue(bids.size() >= 2, "Phải tìm thấy ít nhất 2 lượt đặt giá");
  }

  @Test
  @Order(5)
  @DisplayName("Test lấy danh sách bid của một người dùng")
  void testGetBidsByUser() {
    List<BidTransaction> userBids = bidDAO.getBidsByUser(testUserId);
    assertNotNull(userBids);
    assertFalse(userBids.isEmpty(), "Danh sách bid của người dùng không được rỗng");
  }
}