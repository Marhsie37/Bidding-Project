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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailController {

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

    public void setProductData(Product p) {
        this.product = p;
        updateDisplay();
        startTimer();
        loadBidHistory();
    }

    private void startTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
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
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());

        Request request = new Request(CommandType.GET_AUCTION_HISTORY, data);
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> resData = response.getData();
                    List<BidTransaction> history = (List<BidTransaction>) resData.get("history");

                    lvBidHistory.getItems().clear();
                    if (history != null) {
                        for (BidTransaction bid : history) {
                            String bidder = bid.getBidderName();
                            double amount = bid.getBidAmount();
                            String time = bid.getBidTime().toString();
                            lvBidHistory.getItems().add(String.format("%s: %,.0f VNĐ - %s", bidder, amount, time));
                        }
                    }
                }
            });
        });
    }

    @FXML
    public void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(txtBidAmount.getText().trim());

            if (amount <= product.getCurrentPrice()) {
                showAlert("Lỗi", "Giá đặt phải cao hơn giá hiện tại (" + product.getCurrentPrice() + ")!");
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
                        double newPrice = (double) resData.get("currentPrice");
                        product.setCurrentPrice(newPrice);

                        // ✅ CẬP NHẬT ENDTIME NẾU CÓ GIA HẠN
                        if (resData.containsKey("newEndTime")) {
                            String newEndTimeStr = (String) resData.get("newEndTime");
                            LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
                            product.setEndTime(newEndTime);
                            startTimer(); // Refresh đồng hồ ngay lập tức
                        }

                        updateDisplay();
                        loadBidHistory();
                        showAlert("Thành công", "Đặt giá thành công!");
                        txtBidAmount.clear();
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void goToMain(ActionEvent event) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/ProductListController.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void handleStartAutoBid(ActionEvent event) {
        try {
            double maxPrice = Double.parseDouble(txtMaxAutoPrice.getText().trim());
            double increment = Double.parseDouble(txtIncrement.getText().trim());

            if (maxPrice <= product.getCurrentPrice()) {
                showAlert("Lỗi", "Max Bid phải lớn hơn giá hiện tại!");
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
        boolean isSubscribed = chkSubscribe.isSelected();
        System.out.println("Theo dõi sản phẩm: " + isSubscribed);
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "HẾT HẠN!";
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        String result = "";
        if (days > 0) result += days + " ngày ";
        if (hours > 0 || days > 0) result += hours + " giờ ";
        if (minutes > 0 || hours > 0 || days > 0) result += minutes + " phút ";
        result += seconds + " giây";
        return result;
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void updateDisplay() {
        lblDetailPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        lblDescription.setText(product.getDescription() != null ? product.getDescription() : "Không có mô tả.");

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                imgDetail.setImage(new Image(product.getImageUrl(), true));
            } catch (Exception e) {
                System.err.println("Lỗi tải ảnh: " + e.getMessage());
            }
        }
    }
}