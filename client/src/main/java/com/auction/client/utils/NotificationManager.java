package com.auction.client.utils;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Response;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class NotificationManager {

    private static NotificationManager instance;
    private Stage mainStage;
    private SocketClient socketClient;

    // Lưu handlers cũ để restore sau (nếu cần)
    private Consumer<Response> oldBidHandler;
    private Consumer<Response> oldAuctionEndHandler;
    private Consumer<Response> oldExtendedHandler;

    private NotificationManager() {}

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
    }

    private void setupNotificationListeners() {
        // Lưu handler cũ nếu có
        oldBidHandler = socketClient.getResponseHandler(CommandType.BID_UPDATE);
        oldAuctionEndHandler = socketClient.getResponseHandler(CommandType.AUCTION_END);
        oldExtendedHandler = socketClient.getResponseHandler(CommandType.AUCTION_EXTENDED);

        // Đăng ký handler mới (ghi đè lên)
        socketClient.registerResponseHandler(CommandType.BID_UPDATE, this::handleBidResponse);
        socketClient.registerResponseHandler(CommandType.AUCTION_END, this::handleAuctionEndResponse);
        socketClient.registerResponseHandler(CommandType.AUCTION_EXTENDED, this::handleExtendedResponse);
    }

    private void handleBidResponse(Response response) {
        // Gọi handler cũ nếu có
        if (oldBidHandler != null) {
            oldBidHandler.accept(response);
        }

        if (response.isSuccess()) {
            Platform.runLater(() -> {
                var data = response.getData();
                int productId = ((Number) data.get("productId")).intValue();
                String bidderName = (String) data.get("bidderName");
                double bidAmount = ((Number) data.get("bidAmount")).doubleValue();

                String message = String.format("🔔 Người dùng %s vừa đặt giá %,d VNĐ cho phiên đấu giá #%d",
                        bidderName, (long) bidAmount, productId);

                NotificationToast.show(mainStage, message, NotificationToast.NotificationType.BID);
            });
        }
    }

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

                String message = String.format("🏆 Phiên đấu giá #%d đã kết thúc. Người thắng: %s với giá %,d VNĐ",
                        productId, winnerName, (long) finalPrice);

                NotificationToast.show(mainStage, message, NotificationToast.NotificationType.AUCTION_END);
            });
        }
    }

    private void handleExtendedResponse(Response response) {
        if (oldExtendedHandler != null) {
            oldExtendedHandler.accept(response);
        }

        if (response.isSuccess()) {
            Platform.runLater(() -> {
                var data = response.getData();
                int productId = ((Number) data.get("productId")).intValue();
                String newEndTimeStr = (String) data.get("newEndTime");

                String message;
                try {
                    LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
                    String formattedTime = newEndTime.format(formatter);
                    message = String.format("⏰ Phiên đấu giá #%d được gia hạn đến %s", productId, formattedTime);
                } catch (Exception e) {
                    message = String.format("⏰ Phiên đấu giá #%d được gia hạn", productId);
                }

                NotificationToast.show(mainStage, message, NotificationToast.NotificationType.TIME_EXTEND);
            });
        }
    }

    public void showSubscribeNotification(int auctionId) {
        String message = String.format("✅ Bạn đã theo dõi phiên đấu giá #%d", auctionId);
        NotificationToast.show(mainStage, message, NotificationToast.NotificationType.SUBSCRIBE);
    }

    public void showUnsubscribeNotification(int auctionId) {
        String message = String.format("❌ Bạn đã ngừng theo dõi phiên đấu giá #%d", auctionId);
        NotificationToast.show(mainStage, message, NotificationToast.NotificationType.UNSUBSCRIBE);
    }

    // Optional: cleanup khi thoát app
    public void shutdown() {
        // Restore lại handlers cũ nếu muốn
        if (oldBidHandler != null) {
            socketClient.registerResponseHandler(CommandType.BID_UPDATE, oldBidHandler);
        }
        if (oldAuctionEndHandler != null) {
            socketClient.registerResponseHandler(CommandType.AUCTION_END, oldAuctionEndHandler);
        }
        if (oldExtendedHandler != null) {
            socketClient.registerResponseHandler(CommandType.AUCTION_EXTENDED, oldExtendedHandler);
        }
    }
}