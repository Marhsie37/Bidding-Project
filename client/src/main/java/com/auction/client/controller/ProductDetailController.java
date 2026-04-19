package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ProductDetailController {
    @FXML private ImageView imgDetail;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailTimer;
    @FXML private TextField txtBidAmount;

    private Product product;
    private Runnable onPriceChangeCallback;


    public void setOnPriceChange(Runnable callback) {
        this.onPriceChangeCallback = callback;
    }

    public void setProductData(Product p) {
        this.product = p;
        lblDetailPrice.setText(p.getPrice() + " $");
        try {
            imgDetail.setImage(new Image(p.getImageUrl(), true));
        } catch (Exception e) {}
        startTimer();
    }

    @FXML
    void handlePlaceBid() {
        try {
            double giaMoi = Double.parseDouble(txtBidAmount.getText().trim());
            double giaHienTai = Double.parseDouble(product.getPrice());

            if (giaMoi > giaHienTai) {

                product.setPrice(String.valueOf(giaMoi));

                lblDetailPrice.setText(giaMoi + " $");
                txtBidAmount.clear();


                if (onPriceChangeCallback != null) {
                    onPriceChangeCallback.run();
                }
            } else {
                hienThongBao("Lỗi", "Giá mới phải lớn hơn " + giaHienTai + " $");
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
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}