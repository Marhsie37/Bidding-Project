package com.auction.client.utils;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Response;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

public class NotificationManager {

  private static NotificationManager instance;
  private Stage mainStage;
  private SocketClient socketClient;

  // Hàng đợi thông báo
  private Queue<QueuedNotification> notificationQueue = new LinkedList<>();
  private boolean isShowing = false;

  // 🟢 CHỐNG SPAM: Lưu lần cuối thông báo cho mỗi loại + key
  private Map<String, Long> lastNotifiedTime = new HashMap<>();
  private static final int SPAM_DELAY_MS = 3000; // 3 giây

  private static class QueuedNotification {
    String message;
    NotificationToast.NotificationType type;

    QueuedNotification(String message, NotificationToast.NotificationType type) {
      this.message = message;
      this.type = type;
    }
  }

  private Consumer<Response> oldBidHandler;
  private Consumer<Response> oldAuctionEndHandler;
  private Consumer<Response> oldExtendedHandler;

  private NotificationManager() {
  }

  public static NotificationManager getInstance() {
    if (instance == null) {
      instance = new NotificationManager();
    }
    return instance;
  }

  public void init(Stage stage, SocketClient client) {
    this.mainStage = stage;
    this.socketClient = client;
    setupNotificationListeners();
    System.out.println("✅✅✅ NotificationManager.init() ĐƯỢC GỌI!");
  }

  // 🟢 Kiểm tra có bị spam không
  private boolean isSpam(String uniqueKey) {
    long now = System.currentTimeMillis();
    Long lastTime = lastNotifiedTime.get(uniqueKey);
    if (lastTime != null && (now - lastTime) < SPAM_DELAY_MS) {
      System.out.println("🚫 Bỏ qua thông báo trùng: " + uniqueKey);
      return true;
    }
    lastNotifiedTime.put(uniqueKey, now);
    return false;
  }

  public void showNotification(String message, NotificationToast.NotificationType type) {
    notificationQueue.add(new QueuedNotification(message, type));
    processNextNotification();
  }

  private void processNextNotification() {
    if (isShowing) return;
    if (notificationQueue.isEmpty()) return;

    isShowing = true;
    QueuedNotification notif = notificationQueue.poll();

    NotificationToast.show(mainStage, notif.message, notif.type);

    PauseTransition pause = new PauseTransition(Duration.millis(800));
    pause.setOnFinished(e -> {
      isShowing = false;
      processNextNotification();
    });
    pause.play();
  }

  private void setupNotificationListeners() {
    oldBidHandler = socketClient.getResponseHandler(CommandType.BID_UPDATE);
    oldAuctionEndHandler = socketClient.getResponseHandler(CommandType.AUCTION_END);
    oldExtendedHandler = socketClient.getResponseHandler(CommandType.AUCTION_EXTENDED);

    socketClient.addResponseHandler(CommandType.BID_UPDATE, this::handleBidResponse);
    socketClient.addResponseHandler(CommandType.AUCTION_END, this::handleAuctionEndResponse);
    socketClient.addResponseHandler(CommandType.AUCTION_EXTENDED, this::handleExtendedResponse);
  }

  // 🟢 XỬ LÝ BID_UPDATE
  private void handleBidResponse(Response response) {
    if (oldBidHandler != null) {
      oldBidHandler.accept(response);
    }

    if (response.isSuccess()) {
      Platform.runLater(() -> {
        var data = response.getData();
        int productId = ((Number) data.get("productId")).intValue();
        String bidderName = (String) data.get("bidderName");
        double bidAmount = ((Number) data.get("bidAmount")).doubleValue();

        // 🟢 Tạo key duy nhất: productId + bidderName + bidAmount
        String uniqueKey = "BID_" + productId + "_" + bidderName + "_" + bidAmount;
        if (isSpam(uniqueKey)) return;

        String message = String.format("💰 %s vừa đặt %,d VNĐ", bidderName, (long) bidAmount);
        showNotification(message, NotificationToast.NotificationType.BID);
      });
    }
  }

  // 🟢 XỬ LÝ AUCTION_END - KHÔNG BỊ SPAM
  private void handleAuctionEndResponse(Response response) {
    if (oldAuctionEndHandler != null) {
      oldAuctionEndHandler.accept(response);
    }

    if (response.isSuccess()) {
      Platform.runLater(() -> {
        var data = response.getData();
        int productId = ((Number) data.get("productId")).intValue();
        String winnerName = (String) data.get("winnerName");
        double finalPrice = ((Number) data.get("finalPrice")).doubleValue();

        // 🟢 Tạo key duy nhất cho AUCTION_END
        String uniqueKey = "END_" + productId;
        if (isSpam(uniqueKey)) return;

        String message = String.format("🏆 Kết thúc #%d - %s thắng với %,d VNĐ",
                productId, winnerName, (long) finalPrice);
        showNotification(message, NotificationToast.NotificationType.AUCTION_END);
      });
    }
  }

  // 🟢 XỬ LÝ AUCTION_EXTENDED
  private void handleExtendedResponse(Response response) {
    if (oldExtendedHandler != null) {
      oldExtendedHandler.accept(response);
    }

    if (response.isSuccess()) {
      Platform.runLater(() -> {
        var data = response.getData();
        int productId = ((Number) data.get("productId")).intValue();
        String newEndTimeStr = (String) data.get("newEndTime");

        // 🟢 Tạo key duy nhất cho EXTENDED
        String uniqueKey = "EXTEND_" + productId;
        if (isSpam(uniqueKey)) return;

        String message;
        try {
          LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
          String formattedTime = newEndTime.format(formatter);
          message = String.format("⏰ Phiên #%d gia hạn đến %s", productId, formattedTime);
        } catch (Exception e) {
          message = String.format("⏰ Phiên #%d được gia hạn 60s!", productId);
        }

        showNotification(message, NotificationToast.NotificationType.TIME_EXTEND);
      });
    }
  }

  public void showSubscribeNotification(int auctionId) {
    String uniqueKey = "SUBSCRIBE_" + auctionId;
    if (isSpam(uniqueKey)) return;

    String message = String.format("✅ Đã theo dõi phiên #%d", auctionId);
    showNotification(message, NotificationToast.NotificationType.SUBSCRIBE);
  }

  public void showUnsubscribeNotification(int auctionId) {
    String uniqueKey = "UNSUBSCRIBE_" + auctionId;
    if (isSpam(uniqueKey)) return;

    String message = String.format("❌ Đã ngừng theo dõi phiên #%d", auctionId);
    showNotification(message, NotificationToast.NotificationType.UNSUBSCRIBE);
  }

  public void shutdown() {
    if (oldBidHandler != null) {
      socketClient.addResponseHandler(CommandType.BID_UPDATE, oldBidHandler);
    }
    if (oldAuctionEndHandler != null) {
      socketClient.addResponseHandler(CommandType.AUCTION_END, oldAuctionEndHandler);
    }
    if (oldExtendedHandler != null) {
      socketClient.addResponseHandler(CommandType.AUCTION_EXTENDED, oldExtendedHandler);
    }
  }
}