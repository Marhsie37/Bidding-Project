package com.auction.client.controller;

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

public class RegisterController {

    // ĐÃ BỎ hoField và tenField
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;

    // Nút Cancel hoặc khi tạo thành công thì quay lại Login
    @FXML
    public void goToLoginScreen(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Part1/Login.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.setTitle("Đăng nhập");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Xử lý khi bấm nút Create
    @FXML
    public void handleCreateAccount(ActionEvent event) {
        // Lấy Họ và Tên từ màn hình Name truyền sang
        String ho = Name.hoDangKy;
        String ten = Name.tenDangKy;

        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Kiểm tra trống
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        boolean isSaved = saveUserToDatabase(ho, ten, email, password);

        if (isSaved) {
            // Mẹo: Cập nhật luôn tài khoản hệ thống để bạn có thể test đăng nhập ngay sau khi tạo
            Login.emailHeThong = email;
            Login.passHeThong = password;

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo tài khoản thành công! Nhấn OK để quay lại trang Đăng nhập.");
            goToLoginScreen(event);
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Email này đã được sử dụng. Vui lòng chọn email khác!");
        }
    }

    private boolean saveUserToDatabase(String ho, String ten, String email, String password) {
        String mockExistingEmail = "admin@gmail.com";
        if (email.equalsIgnoreCase(mockExistingEmail)) {
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}