package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

  private BidTransaction bidTransaction;
  private BidTransaction autoBidTransaction;

  @BeforeEach
  void setUp() {
    bidTransaction = new BidTransaction(100, 1, "john_doe", 1500.0, false);
    autoBidTransaction = new BidTransaction(200, 2, "jane_doe", 2500.0, true);
  }

  @Test
  void testDefaultConstructor() {
    BidTransaction emptyTransaction = new BidTransaction();
    assertNotNull(emptyTransaction);
  }

  @Test
  void testParameterizedConstructor() {
    assertEquals(100, bidTransaction.getAuctionId());
    assertEquals(1, bidTransaction.getBidderId());
    assertEquals("john_doe", bidTransaction.getBidderName());
    assertEquals(1500.0, bidTransaction.getBidAmount());
    assertFalse(bidTransaction.isAutoBid());
    assertNotNull(bidTransaction.getBidTime());
  }

  @Test
  void testAutoBidFlag() {
    assertFalse(bidTransaction.isAutoBid());
    assertTrue(autoBidTransaction.isAutoBid());
  }

  @Test
  void testBidTimeIsSetOnConstruction() {
    LocalDateTime before = LocalDateTime.now();
    BidTransaction newTransaction = new BidTransaction(101, 3, "new_user", 500.0, false);
    LocalDateTime after = LocalDateTime.now();

    assertNotNull(newTransaction.getBidTime());
    assertTrue(newTransaction.getBidTime().isAfter(before) || newTransaction.getBidTime().equals(before));
    assertTrue(newTransaction.getBidTime().isBefore(after) || newTransaction.getBidTime().equals(after));
  }

  @Test
  void testSettersAndGetters() {
    LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30);

    bidTransaction.setId(999);
    bidTransaction.setAuctionId(500);
    bidTransaction.setBidderId(10);
    bidTransaction.setBidderName("updated_user");
    bidTransaction.setBidAmount(3000.0);
    bidTransaction.setBidTime(customTime);
    bidTransaction.setAutoBid(true);

    assertEquals(999, bidTransaction.getId());
    assertEquals(500, bidTransaction.getAuctionId());
    assertEquals(10, bidTransaction.getBidderId());
    assertEquals("updated_user", bidTransaction.getBidderName());
    assertEquals(3000.0, bidTransaction.getBidAmount());
    assertEquals(customTime, bidTransaction.getBidTime());
    assertTrue(bidTransaction.isAutoBid());
  }

  @Test
  void testMultipleBids() {
    BidTransaction bid1 = new BidTransaction(100, 1, "user1", 100.0, false);
    BidTransaction bid2 = new BidTransaction(100, 2, "user2", 150.0, false);
    BidTransaction bid3 = new BidTransaction(100, 1, "user1", 200.0, true);

    assertFalse(bid1.isAutoBid());
    assertFalse(bid2.isAutoBid());
    assertTrue(bid3.isAutoBid());

    assertEquals(100.0, bid1.getBidAmount());
    assertEquals(150.0, bid2.getBidAmount());
    assertEquals(200.0, bid3.getBidAmount());
  }

  @Test
  void testBidAmountPrecision() {
    bidTransaction.setBidAmount(1234.56);
    assertEquals(1234.56, bidTransaction.getBidAmount(), 0.001);

    bidTransaction.setBidAmount(999.99);
    assertEquals(999.99, bidTransaction.getBidAmount(), 0.001);
  }

  @Test
  void testSameAuctionMultipleBidders() {
    int auctionId = 300;

    BidTransaction bidder1 = new BidTransaction(auctionId, 1, "bidder1", 1000.0, false);
    BidTransaction bidder2 = new BidTransaction(auctionId, 2, "bidder2", 1200.0, true);
    BidTransaction bidder3 = new BidTransaction(auctionId, 1, "bidder1", 1300.0, true);

    assertEquals(auctionId, bidder1.getAuctionId());
    assertEquals(auctionId, bidder2.getAuctionId());
    assertEquals(auctionId, bidder3.getAuctionId());

    assertEquals(1, bidder1.getBidderId());
    assertEquals(2, bidder2.getBidderId());
    assertEquals(1, bidder3.getBidderId());
  }
}