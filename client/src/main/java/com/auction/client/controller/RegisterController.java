package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton; // Dùng MenuButton theo ảnh Scene Builder
import javafx.scene.control.MenuItem;   // Dùng MenuItem để tạo lựa chọn
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;

    // Đổi từ ComboBox sang MenuButton cho khớp với Scene Builder của bạn
    @FXML private MenuButton roleMenuButton;

    private String selectedRole = ""; // Biến này dùng để lưu Role đã chọn

    @FXML
    public void initialize() {
        // Tạo các lựa chọn cho MenuButton
        MenuItem bidderItem = new MenuItem("Bidder");
        MenuItem sellerItem = new MenuItem("Seller");

        // Xử lý khi chọn "Bidder"
        bidderItem.setOnAction(e -> {
            selectedRole = "Bidder";
            roleMenuButton.setText("Bidder"); // Hiển thị chữ đã chọn lên mặt nút
        });

        // Xử lý khi chọn "Seller"
        sellerItem.setOnAction(e -> {
            selectedRole = "Seller";
            roleMenuButton.setText("Seller");
        });

        // Thêm các mục vào MenuButton
        roleMenuButton.getItems().setAll(bidderItem, sellerItem);
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        String username = usernameField.getText().trim();
        String fullname = fullnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Lấy giá trị từ biến selectedRole thay vì getValue()
        String role = selectedRole;

        // Kiểm tra xem đã chọn Role chưa và các trường khác có trống không
        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() || role.isEmpty()) {

            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Lưu người dùng vào danh sách tổng trong DataManager
        User newUser = new User(username, fullname, email, password, role);
        DataManager.allUsers.add(newUser);

        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản " + username + " đã được tạo!");
        goToLoginScreen(event);
    }

    // Các hàm phụ giữ nguyên
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void goToLoginScreen(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Part1/LoginController.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}