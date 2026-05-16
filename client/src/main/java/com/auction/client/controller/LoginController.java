package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class LoginController {

    @FXML private TextField userNameField;
    @FXML private TextField passwordField;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = userNameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        SocketClient socketClient = SocketClient.getInstance();

        // ✅ QUAN TRỌNG: Ngắt kết nối cũ trước khi tạo kết nối mới
        if (socketClient.isConnected()) {
            System.out.println("🔌 Đang ngắt kết nối cũ...");
            socketClient.disconnect();
        }

        try {
            System.out.println("🔌 Đang kết nối đến server...");
            socketClient.connect();
            System.out.println("✅ Đã kết nối server");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
            return;
        }

        socketClient.login(username, password, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    handleLoginSuccess(event, response);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", response.getMessage());
                }
            });
        });
    }

    private void handleLoginSuccess(ActionEvent event, Response response) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.getData();
            String role = "";
            if (data != null && data.containsKey("userData")) {
                Map<String, Object> userData = (Map<String, Object>) data.get("userData");
                if (userData != null && userData.get("role") != null) {
                    role = userData.get("role").toString().trim().toUpperCase();
                }
            }

            System.out.println("Role xác định được: [" + role + "]");

            String fxmlPath;
            String windowTitle;

            switch (role) {
                case "ADMIN":
                    fxmlPath = "/com/auction/client/view/Admin.fxml";
                    windowTitle = "Hệ thống đấu giá - Quản trị viên";
                    break;
                case "SELLER":
                    fxmlPath = "/com/auction/client/view/Selling.fxml";
                    windowTitle = "Hệ thống đấu giá - Người bán";
                    break;
                case "BIDDER":
                    fxmlPath = "/com/auction/client/view/ProductListController.fxml";
                    windowTitle = "Hệ thống đấu giá - Người mua";
                    break;
                default:
                    fxmlPath = "/com/auction/client/view/ProductListController.fxml";
                    windowTitle = "Hệ thống đấu giá";
                    break;
            }

            // ❌ BỎ DÒNG NÀY - NÓ GÂY LỖI
            // SocketClient.getInstance().clearHandlers();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(root));
            window.setTitle(windowTitle);
            window.centerOnScreen();
            window.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện: " + e.getMessage());
        }
    }

    @FXML
    public void goToRegisterScreen(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/view/RegisterController.fxml"));
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(registerRoot));
            window.setTitle("Đăng ký tài khoản");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng ký!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}