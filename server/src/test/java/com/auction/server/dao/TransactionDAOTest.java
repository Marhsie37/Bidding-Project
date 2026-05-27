package com.auction.server.dao;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionDAOTest {
  private TransactionDAO transactionDAO;
  private UserDAO userDAO;
  private int testUserId;
  private static final Logger logger = LoggerFactory.getLogger(TransactionDAOTest.class);

  @BeforeAll
  void setup() throws Exception {
    transactionDAO = new TransactionDAO();
    userDAO = new UserDAO();

    String tempUser = "trans_user_" + System.currentTimeMillis();
    userDAO.createUser(tempUser, "123", tempUser + "@test.com", "Trans Tester", "BIDDER");
    testUserId = userDAO.findByUsername(tempUser).getId();
  }

  @Test
  @Order(1)
  @DisplayName("Test ghi log nạp tiền thành công")
  void testLogDeposit() {
    boolean result = transactionDAO.logTransaction(
            testUserId,
            500000.0,
            "DEPOSIT",
            "Nạp tiền qua chuyển khoản ngân hàng"
    );
    assertTrue(result);
  }

  @Test
  @Order(2)
  @DisplayName("Test ghi log trừ tiền đặt giá (BID_HOLD)")
  void testLogBidHold() {
    boolean result = transactionDAO.logTransaction(
            testUserId,
            -150000.0,
            "BID_HOLD",
            "Tạm giữ tiền cho phiên đấu giá Laptop"
    );
    assertTrue(result);
  }

  @Test
  @Order(3)
  @DisplayName("Kiểm tra dữ liệu thực tế trong Database H2")
  void testVerifyDataInDb() {
    String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = " + testUserId;
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      if (rs.next()) {
        int count = rs.getInt(1);
        assertEquals(2, count);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}