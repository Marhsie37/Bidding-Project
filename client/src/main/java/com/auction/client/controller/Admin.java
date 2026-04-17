package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Admin {
    @FXML private VBox vBoxDisplay; // Đây là VBox nằm trong ScrollPane của bạn

    @FXML
    public void initialize() {
        renderUserList();
    }

    public void renderUserList() {
        vBoxDisplay.getChildren().clear(); // Xóa trắng để vẽ lại từ đầu
        vBoxDisplay.setSpacing(10);

        for (User user : DataManager.allUsers) {
            // Không hiển thị chính Admin trong danh sách quản lý
            if (user.getRole().equals("ADMIN")) continue;

            HBox userRow = new HBox(20);
            userRow.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: white;");

            Label infoLabel = new Label("Tên: " + user.getFullname() + " | Vai trò: " + user.getRole() + " | Trạng thái: " + user.getStatus());
            infoLabel.setMinWidth(300);

            Button banBtn = new Button(user.getStatus().equals("ACTIVE") ? "Ban" : "Unban");
            banBtn.setStyle(user.getStatus().equals("ACTIVE") ? "-fx-background-color: red; -fx-text-fill: white;" : "-fx-background-color: green; -fx-text-fill: white;");

            // Xử lý sự kiện khi nhấn nút Ban
            banBtn.setOnAction(e -> {
                if (user.getStatus().equals("ACTIVE")) {
                    user.setStatus("BANNED");
                } else {
                    user.setStatus("ACTIVE");
                }
                renderUserList(); // Vẽ lại danh sách để cập nhật chữ trên Label và Button
            });

            userRow.getChildren().addAll(infoLabel, banBtn);
            vBoxDisplay.getChildren().add(userRow);
        }
    }
}