package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailController {

    private static final Logger logger = LoggerFactory.getLogger(ProductDetailController.class);

    @FXML private ImageView imgDetail;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Label lblDescription;
    @FXML private TextField txtMaxAutoPrice;
    @FXML private TextField txtIncrement;
    @FXML private CheckBox chkSubscribe;
    @FXML private ListView<String> lvBidHistory;

    private Product product;
    private Timeline timerTimeline;
    private boolean isSubscribed = false;

    public void setProductData(Product p) {
        this.product = p;
        updateDisplay();
        startTimer();
        loadBidHistory();
        registerRealtimeHandlers();
    }

    private void startTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (product == null) return;
            int remaining = product.getRemainingSeconds();
            if (remaining <= 0) {
                lblDetailTimer.setText("HẾT HẠN!");
                timerTimeline.stop();
            } else {
                lblDetailTimer.setText(formatTime(remaining));
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void loadBidHistory() {
        if (product == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());

        Request request = new Request(CommandType.GET_AUCTION_HISTORY, data);
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> resData = response.getData();
                    Object historyObj = resData.get("history");

                    lvBidHistory.getItems().clear();

                    if (historyObj instanceof List) {
                        List<?> rawList = (List<?>) historyObj;
                        for (Object obj : rawList) {
                            if (obj instanceof BidTransaction) {
                                BidTransaction bid = (BidTransaction) obj;
                                String bidder = bid.getBidderName();
                                double amount = bid.getBidAmount();
                                String time = bid.getBidTime() != null ? bid.getBidTime().toString() : "";
                                lvBidHistory.getItems().add(String.format("%s: %,.0f VNĐ - %s", bidder, amount, time));
                            }
                        }
                    }

                    if (lvBidHistory.getItems().isEmpty()) {
                        lvBidHistory.getItems().add("Chưa có lượt đặt giá nào");
                    }
                } else {
                    lvBidHistory.getItems().add("Không thể tải lịch sử đấu giá");
                }
            });
        });
    }

    @FXML
    public void  handlePlaceBid() {
        if (product == null) {
            showAlert("Lỗi", "Không có thông tin sản phẩm!");
            return;
        }

        try {
            double amount = Double.parseDouble(txtBidAmount.getText().trim());

            if (amount <= product.getCurrentPrice()) {
                showAlert("Lỗi", "Giá đặt phải cao hơn giá hiện tại (" + String.format("%,.0f", product.getCurrentPrice()) + " VNĐ)!");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.getId());
            data.put("bidAmount", amount);


            Request req = new Request(CommandType.PLACE_BID, data);
            SocketClient.getInstance().sendRequestAsync(req, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Map<String, Object> resData = response.getData();

                        // Xử lý an toàn currentPrice
                        Object priceObj = resData.get("currentPrice");
                        if (priceObj instanceof Number) {
                            double newPrice = ((Number) priceObj).doubleValue();
                            product.setCurrentPrice(newPrice);
                        }

                        // Xử lý gia hạn thời gian
                        if (resData.containsKey("newEndTime")) {
                            String newEndTimeStr = (String) resData.get("newEndTime");
                            try {
                                LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
                                product.setEndTime(newEndTime);
                                startTimer();
                            } catch (Exception e) {
                                logger.error("Lỗi parse newEndTime: {}", e.getMessage());
                            }
                        }

                        updateDisplay();
                        loadBidHistory();
                        showAlert("Thành công", "Đặt giá thành công!");
                        txtBidAmount.clear();
                    } else {
                        showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Đặt giá thất bại!");
                    }
                });
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void handleStartAutoBid(ActionEvent event) {
        if (product == null) return;

        try {
            double maxPrice = Double.parseDouble(txtMaxAutoPrice.getText().trim());
            double increment = Double.parseDouble(txtIncrement.getText().trim());

            if (maxPrice <= product.getCurrentPrice()) {
                showAlert("Lỗi", "Max Bid phải lớn hơn giá hiện tại (" + String.format("%,.0f", product.getCurrentPrice()) + " VNĐ)!");
                return;
            }

            if (increment <= 0) {
                showAlert("Lỗi", "Bước giá phải lớn hơn 0!");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.getId());
            data.put("maxBid", maxPrice);
            data.put("increment", increment);

            Request req = new Request(CommandType.SET_AUTO_BID, data);
            SocketClient.getInstance().sendRequestAsync(req, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showAlert("Thành công", response.getMessage());
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    @FXML
    public void handleSubscribeAction(ActionEvent event) {
        if (product == null) return;

        isSubscribed = chkSubscribe.isSelected();

        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());
        CommandType cmd = isSubscribed ? CommandType.SUBSCRIBE_AUCTION : CommandType.UNSUBSCRIBE_AUCTION;

        Request req = new Request(cmd, data);
        SocketClient.getInstance().sendRequestAsync(req, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    logger.info("{} sản phẩm thành công", isSubscribed ? "Theo dõi" : "Hủy theo dõi");
                    showAlert("Thông báo", isSubscribed ? "Đã theo dõi sản phẩm!" : "Đã hủy theo dõi sản phẩm!");
                } else {
                    chkSubscribe.setSelected(!isSubscribed);
                    showAlert("Lỗi", response.getMessage());
                }
            });
        });
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "HẾT HẠN!";
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (days > 0) return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateDisplay() {
        if (product == null) return;

        lblDetailPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        lblDescription.setText(product.getDescription() != null ? product.getDescription() : "Không có mô tả.");

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                String url = product.getImageUrl();
                if (!url.startsWith("http") && !url.startsWith("file:")) {
                    url = "file:" + url;
                }
                Image image = new Image(url, true);
                imgDetail.setImage(image);
            } catch (Exception e) {
                logger.error("Lỗi tải ảnh: ", e);
                imgDetail.setImage(null);
            }
        }
    }

    private void registerRealtimeHandlers() {
        if (product == null) return;

        int productId = product.getId();

        // BID_UPDATE: Có người đặt giá mới
        SocketClient.getInstance().setBidUpdateHandler(response -> {
            if (response.getData() == null || product == null) return;
            Map<String, Object> data = response.getData();

            int updatedProductId = ((Number) data.get("productId")).intValue();
            if (updatedProductId != productId) return;

            double newPrice = ((Number) data.get("bidAmount")).doubleValue();
            String bidderName = (String) data.get("bidderName");

            Platform.runLater(() -> {
                product.setCurrentPrice(newPrice);
                updateDisplay();
                loadBidHistory();
                showNotification("💰 Có giá mới!", bidderName + " vừa đặt " + String.format("%,.0f VNĐ", newPrice));
            });
        });

        // AUCTION_END: Phiên kết thúc
        SocketClient.getInstance().setAuctionEndHandler(response -> {
            if (response.getData() == null || product == null) return;
            Map<String, Object> data = response.getData();

            int endedProductId = ((Number) data.get("productId")).intValue();
            if (endedProductId != productId) return;

            String winnerName = (String) data.get("winnerName");
            double finalPrice = ((Number) data.get("finalPrice")).doubleValue();

            Platform.runLater(() -> {
                if (timerTimeline != null) timerTimeline.stop();
                lblDetailTimer.setText("ĐÃ KẾT THÚC");
                lblDetailTimer.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Phiên đấu giá kết thúc");
                alert.setHeaderText("🏆 Kết quả đấu giá");
                alert.setContentText("Người thắng: " + winnerName + "\nGiá cuối: " + String.format("%,.0f VNĐ", finalPrice));
                alert.showAndWait();
            });
        });

        // AUCTION_EXTENDED: Phiên được gia hạn
        SocketClient.getInstance().setAuctionExtendedHandler(response -> {
            if (response.getData() == null || product == null) return;
            Map<String, Object> data = response.getData();

            int extendedProductId = ((Number) data.get("productId")).intValue();
            if (extendedProductId != productId) return;

            String newEndTimeStr = (String) data.get("newEndTime");
            try {
                LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
                Platform.runLater(() -> {
                    product.setEndTime(newEndTime);
                    startTimer();
                    showNotification("⏰ Gia hạn!", "Phiên đấu giá được gia hạn thêm 60 giây!");
                });
            } catch (Exception e) {
                logger.error("Lỗi parse newEndTime: {}", e.getMessage());
            }
        });
    }

    @FXML
    public void goToMain(ActionEvent event) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        // Hủy đăng ký nhận thông báo nếu đang theo dõi
        if (isSubscribed && product != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.getId());
            Request req = new Request(CommandType.UNSUBSCRIBE_AUCTION, data);
            SocketClient.getInstance().sendRequestAsync(req, response -> {});
        }

        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/ProductListController.fxml", this);
        } catch (Exception e) {
            logger.error("Lỗi khi quay về màn hình chính: ", e);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showNotification(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }
}