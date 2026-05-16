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
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;
    @FXML private MenuButton roleMenuButton;

    private String selectedRole = "";

    @FXML
    public void initialize() {
        MenuItem bidderItem = new MenuItem("Bidder");
        MenuItem sellerItem = new MenuItem("Seller");

        bidderItem.setOnAction(e -> {
            selectedRole = "Bidder";
            roleMenuButton.setText("Bidder");
        });

        sellerItem.setOnAction(e -> {
            selectedRole = "Seller";
            roleMenuButton.setText("Seller");
        });

        roleMenuButton.getItems().addAll(bidderItem, sellerItem);
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        System.out.println("--- Bắt đầu xử lý Đăng ký ---");
        try {
            // 1. Kiểm tra null từng biến trước khi lấy text (Tránh bực mình vì NullPointerException)
            if (usernameField == null || passwordField == null || confirmPasswordField == null) {
                System.err.println("LỖI: FXML chưa kết nối đúng với Controller! Kiểm tra fx:id");
                return;
            }

            String user = usernameField.getText();
            String pass = passwordField.getText();
            String confirm = confirmPasswordField.getText();
            String email = emailField.getText();
            String full = fullnameField.getText();

            // 2. Validate dữ liệu
            if (user.isEmpty() || pass.isEmpty() || selectedRole.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đủ User, Pass và chọn Role!");
                return;
            }

            if (!pass.equals(confirm)) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
                return;
            }

            // 3. Gửi dữ liệu
            Map<String, Object> data = new HashMap<>();
            data.put("username", user);
            data.put("password", pass);
            data.put("email", email);
            data.put("fullName", full);
            data.put("role", selectedRole.toUpperCase());

            System.out.println("Gửi request REGISTER lên Server cho: " + user);
            Request request = new Request(CommandType.REGISTER, data);

            SocketClient.getInstance().sendRequestAsync(request, response -> {
                // QUAN TRỌNG: Mọi lệnh hiển thị giao diện phải nằm trong Platform.runLater
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        System.out.println("Server báo thành công!");
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo tài khoản thành công!");
                        goToLoginScreen(event);
                    } else {
                        System.out.println("Server báo thất bại: " + response.getMessage());
                        showAlert(Alert.AlertType.ERROR, "Thất bại", response.getMessage());
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console nếu có crash
        }
    }

    @FXML
    public void goToLoginScreen(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.show();
        } catch (IOException e) {
            System.err.println("Không tìm thấy file LoginController.fxml!");
            e.printStackTrace();
        }
    }






    private void showAlert(Alert.AlertType alertType, String title, String message) {
        // Đảm bảo Alert luôn chạy trên UI Thread để không bị treo
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