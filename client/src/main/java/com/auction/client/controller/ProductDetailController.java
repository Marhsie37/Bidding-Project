package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import java.util.HashMap;
import java.util.Map;

public class ProductDetailController {
    @FXML private ImageView imgDetail;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailTimer;
    @FXML private TextField txtBidAmount;

    @FXML private TextField txtMaxAutoBid;
    @FXML private TextField txtIncrement;
    @FXML private CheckBox chkSubscribe;
    @FXML private Button btnStartAuto;

    @FXML private Label lblDescription;
    @FXML private ListView<String> lvBidHistory;

    private Product product;
    private Runnable onPriceChangeCallback;
    private Product prod;
    private String currentUserName = "Người dùng hiện tại";



    public void setOnPriceChange(Runnable callback) {
        this.onPriceChangeCallback = callback;
    }

    private void updateBidHistoryUI() {
        if (lvBidHistory != null) {
            lvBidHistory.getItems().clear();
            lvBidHistory.getItems().addAll(product.getBidHistory());
        }
    }

    public void setProductData(Product p) {
        this.product = p;
        lblDetailPrice.setText(p.getPrice() + " VNĐ");
        if (lblDescription != null) {
            lblDescription.setText(p.getDescription());
        }

        updateBidHistoryUI();
        try {
            imgDetail.setImage(new Image(p.getImageUrl(), true));
        } catch (Exception e) {}
        startTimer();
    }

    @FXML
    void handleSubscribeAction() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getName());
        data.put("username", currentUserName);

        if (chkSubscribe.isSelected()) {

            hienThongBao("Thông báo", "Bạn sẽ nhận được tin nhắn khi có người đặt giá mới cho: " + product.getName());
        } else {

            hienThongBao("Thông báo", "Đã hủy nhận thông báo cho sản phẩm này.");
        }
    }

    @FXML
    void handleStartAutoBid() {
        try {

            String maxPriceStr = txtMaxAutoBid.getText().trim();
            String incrementStr = txtIncrement.getText().trim();

            if (maxPriceStr.isEmpty() || incrementStr.isEmpty()) {
                hienThongBao("Lỗi", "Vui lòng nhập đầy đủ Giá trần và Bước nhảy!");
                return;
            }

            double maxPrice = Double.parseDouble(maxPriceStr);
            double userIncrement = Double.parseDouble(incrementStr);
            double giaHienTai = Double.parseDouble(product.getPrice());

            if (maxPrice <= giaHienTai) {
                hienThongBao("Lỗi", "Giá tối đa phải lớn hơn giá hiện tại!");
                return;
            }
            if (userIncrement <= 0) {
                hienThongBao("Lỗi", "Bước nhảy phải lớn hơn 0!");
                return;
            }


            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.getName()); // Hoặc ID sản phẩm nếu có
            data.put("maxBid", maxPrice);
            data.put("increment", userIncrement);
            data.put("bidder", currentUserName);




            product.setIncrement(userIncrement);
            hienThongBao("Thành công", "Đã bật Auto Bid.\nGiá trần: " + maxPrice + " VNĐ\nBước nhảy: " + userIncrement + " VNĐ");

        } catch (NumberFormatException e) {
            hienThongBao("Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }


    public void updateFromRemote(double newPrice, String winnerName) {
        javafx.application.Platform.runLater(() -> {
            lblDetailPrice.setText(newPrice + " VNĐ");
            product.setPrice(String.valueOf(newPrice));
            product.addBid(winnerName, newPrice);
            updateBidHistoryUI();


            if (winnerName.equals(currentUserName)) {
                lblDetailPrice.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblDetailPrice.setStyle("-fx-text-fill: red;");
            }
        });
    }

    @FXML
    public void handlePlaceBid() {
        try {
            double giaMoi = Double.parseDouble(txtBidAmount.getText().trim());
            double giaHienTai = Double.parseDouble(product.getPrice());

            if (giaMoi > giaHienTai) {

                product.setPrice(String.valueOf(giaMoi));


                product.addBid(currentUserName, giaMoi);


                lblDetailPrice.setText(giaMoi + " VNĐ");
                updateBidHistoryUI(); // Làm mới danh sách hiển thị

                txtBidAmount.clear();

                if (onPriceChangeCallback != null) {
                    onPriceChangeCallback.run();
                }
            } else {
                hienThongBao("Lỗi", "Giá mới phải lớn hơn " + giaHienTai + " VNĐ");
            }
        } catch (Exception e) {
            hienThongBao("Lỗi", "Vui lòng nhập số hợp lệ");
        }
    }

    private void startTimer() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblDetailTimer.setText("Còn lại: " + product.getRemainingSeconds() + " giây");
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    public void openWindow(Parent root) {
        Stage stage = new Stage();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setResizable(true);
        stage.show();
    }

    private void hienThongBao(String title, String content) {
        Alert.AlertType type = title.contains("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Bỏ tiêu đề phụ cho gọn
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void goToMain(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Part1/ProductListController.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}