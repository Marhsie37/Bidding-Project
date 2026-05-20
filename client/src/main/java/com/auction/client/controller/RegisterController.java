package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;
    @FXML private MenuButton roleMenuButton;

    private String selectedRole = "";

    @FXML
    public void initialize() {
        logger.info("RegisterController initialized");

        MenuItem bidderItem = new MenuItem("Bidder");
        MenuItem sellerItem = new MenuItem("Seller");

        bidderItem.setOnAction(e -> {
            selectedRole = "Bidder";
            roleMenuButton.setText("Bidder");
            logger.debug("Selected role: Bidder");
        });

        sellerItem.setOnAction(e -> {
            selectedRole = "Seller";
            roleMenuButton.setText("Seller");
            logger.debug("Selected role: Seller");
        });

        roleMenuButton.getItems().addAll(bidderItem, sellerItem);
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        logger.info("--- Bắt đầu xử lý Đăng ký ---");

        // Kiểm tra FXML binding
        if (usernameField == null || passwordField == null || confirmPasswordField == null) {
            logger.error("LỖI: FXML chưa kết nối đúng với Controller! Kiểm tra fx:id");
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi giao diện, vui lòng thử lại!");
            return;
        }

        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        String email = emailField.getText().trim();
        String full = fullnameField.getText().trim();

        // Validate dữ liệu
        if (user.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tên đăng nhập không được để trống!");
            return;
        }

        if (pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Mật khẩu không được để trống!");
            return;
        }

        if (selectedRole.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn Role (Bidder hoặc Seller)!");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Kiểm tra độ dài mật khẩu
        if (pass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // Kiểm tra email (nếu có nhập)
        if (email != null && !email.isEmpty() && !email.contains("@")) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Email không hợp lệ!");
            return;
        }

        // Gửi dữ liệu lên server
        Map<String, Object> data = new HashMap<>();
        data.put("username", user);
        data.put("password", pass);
        data.put("email", email != null ? email : "");
        data.put("fullName", full != null ? full : "");
        data.put("role", selectedRole.toUpperCase());

        logger.info("Gửi request REGISTER lên Server cho: {}", user);
        Request request = new Request(CommandType.REGISTER, data);

        // Disable nút đăng ký để tránh spam
        Button sourceButton = (Button) event.getSource();
        sourceButton.setDisable(true);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                sourceButton.setDisable(false);

                if (response.isSuccess()) {
                    logger.info("Đăng ký thành công cho user: {}", user);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Tạo tài khoản thành công!\nVui lòng đăng nhập.");
                    goToLoginScreen(event);
                } else {
                    logger.warn("Đăng ký thất bại cho user: {} - {}", user, response.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Thất bại",
                            response.getMessage() != null ? response.getMessage() : "Đăng ký thất bại!");
                }
            });
        });
    }

    @FXML
    public void goToLoginScreen(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", this);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển về màn hình đăng nhập: ", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại màn hình đăng nhập!");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(alertType, title, message));
            return;
        }
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}