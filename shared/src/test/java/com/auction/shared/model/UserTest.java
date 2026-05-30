package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(1, "john_doe", "john@example.com", "John Doe", "BIDDER", 1000.0);
    user.setId(1);
    user.setPassword("password123");
    user.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void testDefaultConstructor() {
    User emptyUser = new User();
    assertNotNull(emptyUser);
  }

  @Test
  void testParameterizedConstructor() {
    assertEquals(1, user.getId());
    assertEquals("john_doe", user.getUsername());
    assertEquals("john@example.com", user.getEmail());
    assertEquals("John Doe", user.getFullName());
    assertEquals("BIDDER", user.getRole());
    assertEquals(1000.0, user.getBalance());
  }

  @Test
  void testSettersAndGetters() {
    user.setId(2);
    user.setUsername("jane_doe");
    user.setEmail("jane@example.com");
    user.setFullName("Jane Doe");
    user.setRole("SELLER");
    user.setBalance(2000.0);
    user.setPassword("newpassword");
    user.setActive(false);

    assertEquals(2, user.getId());
    assertEquals("jane_doe", user.getUsername());
    assertEquals("jane@example.com", user.getEmail());
    assertEquals("Jane Doe", user.getFullName());
    assertEquals("SELLER", user.getRole());
    assertEquals(2000.0, user.getBalance());
    assertEquals("newpassword", user.getPassword());
    assertFalse(user.isActive());
  }

  @Test
  void testIsBidder() {
    assertTrue(user.isBidder());

    user.setRole("SELLER");
    assertFalse(user.isBidder());

    user.setRole("ADMIN");
    assertFalse(user.isBidder());
  }

  @Test
  void testIsSeller() {
    user.setRole("SELLER");
    assertTrue(user.isSeller());

    user.setRole("BIDDER");
    assertFalse(user.isSeller());
  }

  @Test
  void testIsAdmin() {
    user.setRole("ADMIN");
    assertTrue(user.isAdmin());

    user.setRole("BIDDER");
    assertFalse(user.isAdmin());
  }

  @Test
  void testCreatedAt() {
    LocalDateTime now = LocalDateTime.now();
    user.setCreatedAt(now);
    assertEquals(now, user.getCreatedAt());
  }

  @Test
  void testActiveDefault() {
    User newUser = new User();
    assertTrue(newUser.isActive());
  }
}