package com.auction.client.controller;

import com.auction.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.Map;

public class UserItemController {

    @FXML private Label lblInfo;
    @FXML private Button btnBan;
    @FXML private Button btnDelete;

    public void setData(User user) {
        if (user == null) return;

        String statusText = user.getStatus() != null ? user.getStatus() : "ACTIVE";
        lblInfo.setText(user.getUsername() + " | " + user.getFullName() + " | " + user.getEmail() + " | " + user.getRole() + " | " + statusText);

        if ("BANNED".equals(statusText)) {
            btnBan.setText("Unban");
            btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        } else {
            btnBan.setText("Ban");
            btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    // ✅ THÊM METHOD NÀY CHO ADMIN DÙNG
    public void setDataFromMap(Map<String, Object> userMap) {
        String username = (String) userMap.getOrDefault("username", "");
        String fullName = (String) userMap.getOrDefault("fullName", "");
        String email = (String) userMap.getOrDefault("email", "");
        String role = (String) userMap.getOrDefault("role", "");
        String status = (String) userMap.getOrDefault("status", "ACTIVE");

        lblInfo.setText(username + " | " + fullName + " | " + email + " | " + role + " | " + status);

        if ("BANNED".equals(status)) {
            btnBan.setText("Unban");
            btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        } else {
            btnBan.setText("Ban");
            btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    public Button getBtnBan() { return btnBan; }
    public Button getBtnDelete() { return btnDelete; }
}