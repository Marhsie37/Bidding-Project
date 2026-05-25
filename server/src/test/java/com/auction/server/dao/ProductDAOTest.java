package com.auction.server.dao;

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
public class ProductDAOTest {
  private Connection testConn;
  private ProductDAO productDAO;
  private int testSellerId = 1;
  private static int createdProductId;
  private static final Logger logger = LoggerFactory.getLogger(ProductDAOTest.class);

  @BeforeAll
  void setupDatabase() throws Exception {
    testConn = DriverManager.getConnection("jdbc:h2:mem:testdb_product;DB_CLOSE_DELAY=-1");
    try (Statement stmt = testConn.createStatement()) {
      stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
              "id INT PRIMARY KEY AUTO_INCREMENT, " +
              "username VARCHAR(50))");

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
    }
    productDAO = new ProductDAO(testConn);
  }

  @AfterAll
  void tearDown() throws Exception {
    if (testConn != null) {
      testConn.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test tạo sản phẩm mới")
  void testCreateProduct() {
    Product product = new Product();
    product.setName("Laptop Gaming");
    product.setDescription("Core i9, RTX 4090");
    product.setStartingPrice(5000.0);
    product.setSellerId(testSellerId);
    product.setCategory("Electronics");
    product.setDurationHours(24);
    product.setEndTime(LocalDateTime.now().plusHours(24));

    boolean created = productDAO.createProduct(product);

    assertTrue(created, "Sản phẩm phải được tạo thành công");

    createdProductId = product.getId();
    assertTrue(createdProductId > 0, "ID sản phẩm phải lớn hơn 0");
    logger.info("Sản phẩm test được tạo với ID: " + createdProductId);
  }

  @Test
  @Order(2)
  @DisplayName("Test tìm sản phẩm bằng ID")
  void testFindById() {
    Product found = productDAO.findById(createdProductId);

    assertNotNull(found, "Phải tìm thấy sản phẩm có ID: " + createdProductId);
    assertEquals("Laptop Gaming", found.getName());
  }

  @Test
  @Order(3)
  @DisplayName("Test cập nhật giá hiện tại")
  void testUpdateCurrentPrice() {
    boolean updated = productDAO.updateCurrentPrice(createdProductId, 5500.0);

    assertTrue(updated, "Cập nhật giá phải thành công");
    Product found = productDAO.findById(createdProductId);
    assertEquals(5500.0, found.getCurrentPrice(), "Giá mới phải là 5500.0");
  }

  @Test
  @Order(4)
  @DisplayName("Test lấy danh sách sản phẩm đang đấu giá")
  void testGetActiveProducts() {
    productDAO.activateProduct(createdProductId);

    List<Product> activeProducts = productDAO.getActiveProducts();

    boolean found = activeProducts.stream().anyMatch(p -> p.getId() == createdProductId);
    assertTrue(found, "Danh sách active phải chứa sản phẩm vừa kích hoạt");
  }

  @Test
  @Order(5)
  @DisplayName("Test xóa sản phẩm (chế độ Admin)")
  void testAdminDeleteProduct() {
    boolean deleted = productDAO.adminDeleteProduct(createdProductId);
    assertTrue(deleted, "Xóa sản phẩm phải thành công");

    Product found = productDAO.findById(createdProductId);
    assertNull(found, "Sản phẩm phải không còn tồn tại sau khi xóa");
  }
}