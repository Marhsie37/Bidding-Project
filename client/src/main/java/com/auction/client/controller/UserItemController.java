package com.auction.client.controller;

import com.auction.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UserItemController {

    private static final Logger logger = LoggerFactory.getLogger(UserItemController.class);

    @FXML private Label lblInfo;
    @FXML private Button btnBan;
    @FXML private Button btnDelete;

    // ==================== DÙNG CHO USER OBJECT (TỪ DATABASE) ====================

    public void setData(User user) {
        if (user == null) {
            logger.warn("User is null, cannot set data");
            return;
        }

        // Lấy status từ User object
        String statusText = user.getStatus() != null ? user.getStatus() : "ACTIVE";

        // Hiển thị thông tin
        lblInfo.setText(buildInfoText(
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                statusText
        ));

        // Cấu hình nút Ban/Unban
        configureBanButton(statusText);

        logger.debug("Set data for user: {} (status: {})", user.getUsername(), statusText);
    }

    // ==================== DÙNG CHO MAP (TỪ ADMIN RESPONSE) ====================

    public void setDataFromMap(Map<String, Object> userMap) {
        if (userMap == null) {
            logger.warn("UserMap is null, cannot set data");
            return;
        }

        String username = (String) userMap.getOrDefault("username", "");
        String fullName = (String) userMap.getOrDefault("fullName", "");
        String email = (String) userMap.getOrDefault("email", "");
        String role = (String) userMap.getOrDefault("role", "");
        String status = (String) userMap.getOrDefault("status", "ACTIVE");

        lblInfo.setText(buildInfoText(username, fullName, email, role, status));
        configureBanButton(status);

        logger.debug("Set data from map for user: {} (status: {})", username, status);
    }

    // ==================== CẬP NHẬT TRẠNG THÁI ====================

    public void updateStatus(String newStatus) {
        String currentText = lblInfo.getText();
        String[] parts = currentText.split(" \\| ");
        if (parts.length >= 5) {
            parts[4] = newStatus;
            lblInfo.setText(String.join(" | ", parts));
        }
        configureBanButton(newStatus);
        logger.debug("Updated status to: {}", newStatus);
    }

    // ==================== PRIVATE HELPERS ====================

    private String buildInfoText(String username, String fullName, String email, String role, String status) {
        return String.format("%s | %s | %s | %s | %s",
                username != null ? username : "",
                fullName != null ? fullName : "",
                email != null ? email : "",
                role != null ? role : "",
                status != null ? status : "ACTIVE"
        );
    }

    private void configureBanButton(String status) {
        if (status == null) {
            status = "ACTIVE";
        }

        if ("BANNED".equals(status)) {
            btnBan.setText("Unban");
            btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
            btnBan.setUserData("BANNED");
        } else {
            btnBan.setText("Ban");
            btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
            btnBan.setUserData("ACTIVE");
        }
    }

    // ==================== GETTERS ====================

    public Label getLblInfo() {
        return lblInfo;
    }

    public Button getBtnBan() {
        return btnBan;
    }

    public Button getBtnDelete() {
        return btnDelete;
    }

    public boolean isBanned() {
        return "BANNED".equals(btnBan.getUserData());
    }
}