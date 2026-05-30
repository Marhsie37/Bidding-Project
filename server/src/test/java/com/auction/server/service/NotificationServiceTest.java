package com.auction.server.service;

import com.auction.server.ClientHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

  private NotificationService notificationService;
  private TestClientHandler testHandler1;
  private TestClientHandler testHandler2;

  // Test implementation của ClientHandler
  static class TestClientHandler extends ClientHandler {
    private String username;
    private List<Object[]> receivedUpdates = new ArrayList<>();

    public TestClientHandler(String username) {
      super(null); // Socket có thể là null cho test
      this.username = username;
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public void sendBidUpdate(int productId, String bidderName, double bidAmount) {
      receivedUpdates.add(new Object[]{"BID_UPDATE", productId, bidderName, bidAmount});
    }

    @Override
    public void sendAuctionEnd(int productId, int winnerId, String winnerName, double finalPrice) {
      receivedUpdates.add(new Object[]{"AUCTION_END", productId, winnerId, winnerName, finalPrice});
    }

    @Override
    public void sendAuctionExtended(int productId, LocalDateTime newEndTime) {
      receivedUpdates.add(new Object[]{"AUCTION_EXTENDED", productId, newEndTime});
    }

    @Override
    public void sendResponsePublic(com.auction.shared.protocol.Response response) {
      if (response.getCommand() == com.auction.shared.protocol.CommandType.AUCTION_END) {
        java.util.Map<String, Object> data = response.getData();
        int productId = (int) data.get("productId");
        int winnerId = (int) data.get("winnerId");
        String winnerName = (String) data.get("winnerName");
        double finalPrice = (double) data.get("finalPrice");
        receivedUpdates.add(new Object[]{"AUCTION_END", productId, winnerId, winnerName, finalPrice});
      }
    }

    public boolean hasBidUpdate(int productId, double bidAmount) {
      return receivedUpdates.stream().anyMatch(update ->
              update[0].equals("BID_UPDATE") &&
                      (int) update[1] == productId &&
                      (double) update[3] == bidAmount
      );
    }

    public boolean hasAuctionEnd(int productId, int winnerId, double finalPrice) {
      return receivedUpdates.stream().anyMatch(update ->
              update[0].equals("AUCTION_END") &&
                      (int) update[1] == productId &&
                      (int) update[2] == winnerId &&
                      (double) update[4] == finalPrice
      );
    }

    public boolean hasAuctionExtended(int productId) {
      return receivedUpdates.stream().anyMatch(update ->
              update[0].equals("AUCTION_EXTENDED") &&
                      (int) update[1] == productId
      );
    }

    public void clear() {
      receivedUpdates.clear();
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    // Reset singleton instance
    Field instanceField = NotificationService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    notificationService = NotificationService.getInstance();

    // Reset subscribers map
    Field subscribersField = NotificationService.class.getDeclaredField("subscribers");
    subscribersField.setAccessible(true);
    subscribersField.set(notificationService, new ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>>());

    testHandler1 = new TestClientHandler("user1");
    testHandler2 = new TestClientHandler("user2");
  }

  @Test
  void testGetInstance() {
    NotificationService instance1 = NotificationService.getInstance();
    NotificationService instance2 = NotificationService.getInstance();

    assertSame(instance1, instance2);
  }

  @Test
  void testSubscribe() {
    notificationService.subscribe(100, "user1", testHandler1);
    notificationService.notifyBidUpdate(100, "bidder", 500.0);

    assertTrue(testHandler1.hasBidUpdate(100, 500.0));
  }

  @Test
  void testMultipleSubscribers() {
    notificationService.subscribe(200, "user1", testHandler1);
    notificationService.subscribe(200, "user2", testHandler2);

    notificationService.notifyBidUpdate(200, "bidder", 1000.0);

    assertTrue(testHandler1.hasBidUpdate(200, 1000.0));
    assertTrue(testHandler2.hasBidUpdate(200, 1000.0));
  }

  @Test
  void testUnsubscribe() {
    notificationService.subscribe(300, "user1", testHandler1);
    notificationService.unsubscribe(300, "user1");

    notificationService.notifyBidUpdate(300, "bidder", 750.0);

    assertFalse(testHandler1.hasBidUpdate(300, 750.0));
  }

  @Test
  void testNotifyBidUpdateForNonExistentAuction() {
    assertDoesNotThrow(() -> {
      notificationService.notifyBidUpdate(999, "bidder", 100.0);
    });
  }

  @Test
  void testNotifyAuctionEnd() {
    com.auction.server.AuctionServer.getInstance().registerClient("user1", testHandler1);
    com.auction.server.AuctionServer.getInstance().registerClient("user2", testHandler2);

    notificationService.notifyAuctionEnd(400, 42, "winner", 5000.0);

    assertTrue(testHandler1.hasAuctionEnd(400, 42, 5000.0));
    assertTrue(testHandler2.hasAuctionEnd(400, 42, 5000.0));
    
    com.auction.server.AuctionServer.getInstance().unregisterClient("user1");
    com.auction.server.AuctionServer.getInstance().unregisterClient("user2");
  }

  @Test
  void testNotifyAuctionExtended() {
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(1);

    notificationService.subscribe(500, "user1", testHandler1);

    notificationService.notifyAuctionExtended(500, newEndTime);

    assertTrue(testHandler1.hasAuctionExtended(500));
  }

  @Test
  void testSubscribeToMultipleAuctions() {
    notificationService.subscribe(600, "user1", testHandler1);
    notificationService.subscribe(700, "user1", testHandler1);

    notificationService.notifyBidUpdate(600, "bidder1", 100.0);
    notificationService.notifyBidUpdate(700, "bidder2", 200.0);

    assertTrue(testHandler1.hasBidUpdate(600, 100.0));
    assertTrue(testHandler1.hasBidUpdate(700, 200.0));
  }
}