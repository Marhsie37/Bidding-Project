package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSessionTest {

  private AuctionSession auctionSession;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    endTime = LocalDateTime.now().plusHours(24);
    auctionSession = new AuctionSession(1, "iPhone 15 Pro", 999.0, endTime);
    auctionSession.setCurrentWinnerId(0);
    auctionSession.setCurrentWinnerName(null);
  }

  @Test
  void testDefaultConstructor() {
    AuctionSession emptySession = new AuctionSession();
    assertNotNull(emptySession);
    assertNotNull(emptySession.getAutoBids());
    assertTrue(emptySession.getAutoBids().isEmpty());
  }

  @Test
  void testParameterizedConstructor() {
    assertEquals(1, auctionSession.getProductId());
    assertEquals("iPhone 15 Pro", auctionSession.getProductName());
    assertEquals(999.0, auctionSession.getCurrentPrice());
    assertEquals(endTime, auctionSession.getEndTime());
    assertEquals("ACTIVE", auctionSession.getStatus());
    assertEquals(endTime, auctionSession.getScheduledEndTime());
    assertEquals(0, auctionSession.getExtensionCount());
    assertNotNull(auctionSession.getAutoBids());
  }

  @Test
  void testSettersAndGetters() {
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(48);

    auctionSession.setProductId(2);
    auctionSession.setProductName("Samsung Galaxy S24");
    auctionSession.setCurrentPrice(899.0);
    auctionSession.setCurrentWinnerId(10);
    auctionSession.setCurrentWinnerName("winner123");
    auctionSession.setEndTime(newEndTime);
    auctionSession.setStatus("ENDED");
    auctionSession.setScheduledEndTime(newEndTime);
    auctionSession.setExtensionCount(3);

    assertEquals(2, auctionSession.getProductId());
    assertEquals("Samsung Galaxy S24", auctionSession.getProductName());
    assertEquals(899.0, auctionSession.getCurrentPrice());
    assertEquals(10, auctionSession.getCurrentWinnerId());
    assertEquals("winner123", auctionSession.getCurrentWinnerName());
    assertEquals(newEndTime, auctionSession.getEndTime());
    assertEquals("ENDED", auctionSession.getStatus());
    assertEquals(newEndTime, auctionSession.getScheduledEndTime());
    assertEquals(3, auctionSession.getExtensionCount());
  }

  @Test
  void testAddAutoBid() {
    auctionSession.addAutoBid("user1", 1500.0);
    auctionSession.addAutoBid("user2", 2000.0);

    ConcurrentHashMap<String, Double> autoBids = auctionSession.getAutoBids();
    assertEquals(2, autoBids.size());
    assertEquals(1500.0, autoBids.get("user1"));
    assertEquals(2000.0, autoBids.get("user2"));
  }

  @Test
  void testAddAutoBidWithNullUsername() {
    auctionSession.addAutoBid(null, 1500.0);
    assertTrue(auctionSession.getAutoBids().isEmpty());
  }

  @Test
  void testRemoveAutoBid() {
    auctionSession.addAutoBid("user1", 1500.0);
    auctionSession.addAutoBid("user2", 2000.0);

    assertEquals(2, auctionSession.getAutoBids().size());

    auctionSession.removeAutoBid("user1");
    assertEquals(1, auctionSession.getAutoBids().size());
    assertNull(auctionSession.getAutoBids().get("user1"));
    assertEquals(2000.0, auctionSession.getAutoBids().get("user2"));
  }

  @Test
  void testRemoveAutoBidWithNullUsername() {
    auctionSession.addAutoBid("user1", 1500.0);
    auctionSession.removeAutoBid(null);
    assertEquals(1, auctionSession.getAutoBids().size());
  }

  @Test
  void testRemoveNonExistentAutoBid() {
    auctionSession.addAutoBid("user1", 1500.0);
    auctionSession.removeAutoBid("nonexistent");
    assertEquals(1, auctionSession.getAutoBids().size());
  }

  @Test
  void testUpdateAutoBid() {
    auctionSession.addAutoBid("user1", 1500.0);
    assertEquals(1500.0, auctionSession.getAutoBids().get("user1"));

    auctionSession.addAutoBid("user1", 2000.0);
    assertEquals(2000.0, auctionSession.getAutoBids().get("user1"));
  }

  @Test
  void testGetIdReturnsProductId() {
    assertEquals(auctionSession.getProductId(), auctionSession.getId());

    auctionSession.setProductId(999);
    assertEquals(999, auctionSession.getId());
  }

  @Test
  void testAutoBidsConcurrency() {
    ConcurrentHashMap<String, Double> customAutoBids = new ConcurrentHashMap<>();
    customAutoBids.put("user1", 1000.0);
    customAutoBids.put("user2", 2000.0);

    auctionSession.setAutoBids(customAutoBids);
    assertEquals(customAutoBids, auctionSession.getAutoBids());
    assertEquals(2, auctionSession.getAutoBids().size());
  }

  @Test
  void testExtensionCountIncrements() {
    assertEquals(0, auctionSession.getExtensionCount());

    auctionSession.setExtensionCount(1);
    assertEquals(1, auctionSession.getExtensionCount());

    auctionSession.setExtensionCount(5);
    assertEquals(5, auctionSession.getExtensionCount());
  }
}