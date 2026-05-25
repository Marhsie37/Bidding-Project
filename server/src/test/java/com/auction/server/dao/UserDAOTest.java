package com.auction.server.dao;

import com.auction.shared.model.User;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {
  private Connection testConn;
  private UserDAO userDAO;
  private String uniqueSuffix;

  @BeforeAll
  void setup() throws Exception {
    testConn = DriverManager.getConnection("jdbc:h2:mem:testdb_user;DB_CLOSE_DELAY=-1");

    try (Statement stmt = testConn.createStatement()) {
      stmt.execute("CREATE TABLE users (" +
              "id INT PRIMARY KEY AUTO_INCREMENT, " +
              "username VARCHAR(50) UNIQUE, " +
              "password VARCHAR(50), " +
              "email VARCHAR(100) UNIQUE, " +
              "full_name VARCHAR(100), " +
              "role VARCHAR(20), " +
              "balance DOUBLE DEFAULT 0, " +
              "active BOOLEAN DEFAULT TRUE, " +
              "status VARCHAR(20) DEFAULT 'ACTIVE', " +
              "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    userDAO = new UserDAO(testConn);
    uniqueSuffix = String.valueOf(System.currentTimeMillis());
  }

  @AfterAll
  void tearDown() throws Exception {
    if (testConn != null) {
      testConn.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test tạo người dùng mới")
  void testCreateUser() {
    boolean created = userDAO.createUser(
            "user_" + uniqueSuffix,
            "pass123",
            "email_" + uniqueSuffix + "@test.com",
            "Nguyen Van A",
            "BIDDER"
    );
    assertTrue(created, "Phải tạo được user thành công");
  }

  @Test
  @Order(2)
  @DisplayName("Test tìm người dùng theo Username")
  void testFindByUsername() {
    User user = userDAO.findByUsername("user_" + uniqueSuffix);
    assertNotNull(user);
    assertEquals("Nguyen Van A", user.getFullName());
    assertEquals("ACTIVE", user.getStatus());
  }

  @Test
  @Order(3)
  @DisplayName("Test nạp tiền và kiểm tra số dư")
  void testFinanceOperations() {
    User user = userDAO.findByUsername("user_" + uniqueSuffix);
    int userId = user.getId();

    // Nạp 500.0
    userDAO.addFunds(userId, 500.0);
    assertEquals(500.0, userDAO.getBalance(userId), "Số dư sau nạp phải là 500");

    // Trừ 200.0
    boolean deducted = userDAO.deductFunds(userId, 200.0);
    assertTrue(deducted);
    assertEquals(300.0, userDAO.getBalance(userId), "Số dư sau trừ phải là 300");

    // Thử trừ quá số dư
    boolean overDeduct = userDAO.deductFunds(userId, 1000.0);
    assertFalse(overDeduct, "Không được phép trừ quá số dư hiện có");
  }

  @Test
  @Order(4)
  @DisplayName("Test chức năng Ban và Unban người dùng")
  void testBanUnban() {
    User user = userDAO.findByUsername("user_" + uniqueSuffix);
    int userId = user.getId();

    // Ban user
    userDAO.banUser(userId);
    User bannedUser = userDAO.findById(userId);
    assertEquals("BANNED", bannedUser.getStatus(), "Trạng thái phải là BANNED");

    // Unban user
    userDAO.unbanUser(userId);
    User activeUser = userDAO.findById(userId);
    assertEquals("ACTIVE", activeUser.getStatus(), "Trạng thái phải quay lại ACTIVE");
  }

  @Test
  @Order(5)
  @DisplayName("Test lấy danh sách người dùng")
  void testListUsers() {
    List<User> allUsers = userDAO.getAllUsers();
    assertFalse(allUsers.isEmpty());

    List<User> bidders = userDAO.getUsersByRole("BIDDER");
    assertTrue(bidders.stream().anyMatch(u -> u.getUsername().contains(uniqueSuffix)));
  }

  @Test
  @Order(6)
  @DisplayName("Test xóa người dùng")
  void testDeleteUser() {
    User user = userDAO.findByUsername("user_" + uniqueSuffix);
    boolean deleted = userDAO.deleteUser(user.getId());
    assertTrue(deleted);

    User deletedUser = userDAO.findById(user.getId());
    assertNull(deletedUser, "User phải không còn tồn tại sau khi xóa");
  }
}