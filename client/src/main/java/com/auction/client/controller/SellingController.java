package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SellingController {

    @FXML
    private VBox vboxDisplay;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtImageUrl;

    @FXML
    private TextField txtDuration; // Ô nhập số giây đếm ngược

    @FXML
    void handleAddProduct() {

        String name = txtName.getText().trim();
        String price = txtPrice.getText().trim();
        String url = txtImageUrl.getText().trim();
        String durationStr = txtDuration.getText().trim();


        if (name.isEmpty() || price.isEmpty() || url.isEmpty() || durationStr.isEmpty()) {
            System.out.println("Lỗi: Vui lòng nhập đầy đủ thông tin!");
            return;
        }


        int totalSeconds;
        try {
            totalSeconds = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Thời gian phải là con số!");
            return;
        }


        HBox productRow = new HBox(15);
        productRow.setAlignment(Pos.CENTER_LEFT);
        productRow.setStyle(
                "-fx-padding: 10; " +
                        "-fx-background-color: #ffffff; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        );


        ImageView imgView = new ImageView();
        try {
            Image image = new Image(url, true);
            imgView.setImage(image);
            imgView.setFitWidth(80);
            imgView.setFitHeight(80);
            imgView.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Lỗi link ảnh!");
        }


        VBox details = new VBox(5);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label lblPrice = new Label(price + " $");
        lblPrice.setStyle("-fx-text-fill: #e44d26; -fx-font-weight: bold;");


        Label lblCountdown = new Label("Thời gian: " + totalSeconds + "s");
        lblCountdown.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        details.getChildren().addAll(lblName, lblPrice, lblCountdown);


        final int[] timeRemaining = {totalSeconds};
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    timeRemaining[0]--;
                    lblCountdown.setText("Thời gian: " + timeRemaining[0] + "s");

                    if (timeRemaining[0] <= 0) {
                        lblCountdown.setText("HẾT HẠN!");
                        lblCountdown.setStyle("-fx-text-fill: #7f8c8d;");
                        productRow.setOpacity(0.6); // Làm mờ khi hết hạn
                    }
                })
        );
        timeline.setCycleCount(totalSeconds);
        timeline.play();


        Button btnDelete = new Button("Xóa");
        btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            timeline.stop(); // Quan trọng: Dừng đếm ngược trước khi xóa để tiết kiệm RAM
            vboxDisplay.getChildren().remove(productRow);
        });


        productRow.getChildren().addAll(imgView, details, btnDelete);
        vboxDisplay.getChildren().add(productRow);
        vboxDisplay.setSpacing(10);


        clearFields();
    }

    private void clearFields() {
        txtName.clear();
        txtPrice.clear();
        txtImageUrl.clear();
        txtDuration.clear();
        txtName.requestFocus();
    }
}