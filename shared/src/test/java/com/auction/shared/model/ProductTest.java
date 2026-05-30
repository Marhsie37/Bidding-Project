package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

  private Product product;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    product = new Product();
    startTime = LocalDateTime.now();
    endTime = startTime.plusHours(24);

    product.setId(100);
    product.setName("Laptop Gaming");
    product.setDescription("High-end gaming laptop with RTX 4080");
    product.setStartingPrice(1000.0);
    product.setCurrentPrice(1200.0);
    product.setSellerId(1);
    product.setSellerName("TechStore");
    product.setCategory("Electronics");
    product.setImageUrl("laptop.jpg");
    product.setDurationHours(24);
    product.setStartTime(startTime);
    product.setEndTime(endTime);
    product.setStatus("ACTIVE");
    product.setWinnerId(0);
    product.setWinnerName(null);
    product.setCreatedAt(startTime.minusHours(1));
  }

  @Test
  void testDefaultConstructor() {
    Product emptyProduct = new Product();
    assertNotNull(emptyProduct);
  }

  @Test
  void testSettersAndGetters() {
    assertEquals(100, product.getId());
    assertEquals("Laptop Gaming", product.getName());
    assertEquals("High-end gaming laptop with RTX 4080", product.getDescription());
    assertEquals(1000.0, product.getStartingPrice());
    assertEquals(1200.0, product.getCurrentPrice());
    assertEquals(1, product.getSellerId());
    assertEquals("TechStore", product.getSellerName());
    assertEquals("Electronics", product.getCategory());
    assertEquals("laptop.jpg", product.getImageUrl());
    assertEquals(24, product.getDurationHours());
    assertEquals(startTime, product.getStartTime());
    assertEquals(endTime, product.getEndTime());
    assertEquals("ACTIVE", product.getStatus());
    assertEquals(0, product.getWinnerId());
    assertNull(product.getWinnerName());
  }

  @Test
  void testIsActive() {
    assertTrue(product.isActive());

    product.setStatus("INACTIVE");
    assertFalse(product.isActive());

    product.setStatus("ENDED");
    assertFalse(product.isActive());

    product.setStatus(null);
    assertFalse(product.isActive());
  }

  @Test
  void testPriceUpdates() {
    product.setCurrentPrice(1500.0);
    assertEquals(1500.0, product.getCurrentPrice());

    product.setStartingPrice(800.0);
    assertEquals(800.0, product.getStartingPrice());
  }

  @Test
  void testWinnerInfo() {
    product.setWinnerId(5);
    product.setWinnerName("winner_user");

    assertEquals(5, product.getWinnerId());
    assertEquals("winner_user", product.getWinnerName());
  }

  @Test
  void testTimeFields() {
    LocalDateTime newStartTime = LocalDateTime.now().plusDays(1);
    LocalDateTime newEndTime = newStartTime.plusHours(48);

    product.setStartTime(newStartTime);
    product.setEndTime(newEndTime);

    assertEquals(newStartTime, product.getStartTime());
    assertEquals(newEndTime, product.getEndTime());
  }

  @Test
  void testCreatedAt() {
    LocalDateTime createdAt = LocalDateTime.now();
    product.setCreatedAt(createdAt);
    assertEquals(createdAt, product.getCreatedAt());
  }
}