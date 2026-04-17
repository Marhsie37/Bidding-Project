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


    @FXML private MenuButton roleMenuButton;

    private String selectedRole = ""; // Biến này dùng để lưu Role đã chọn

    @FXML
    public void initialize() {

        MenuItem bidderItem = new MenuItem("Bidder");
        MenuItem sellerItem = new MenuItem("Seller");


        bidderItem.setOnAction(e -> {
            selectedRole = "Bidder";
            roleMenuButton.setText("Bidder"); // Hiển thị chữ đã chọn lên mặt nút
        });

        sellerItem.setOnAction(e -> {
            selectedRole = "Seller";
            roleMenuButton.setText("Seller");
        });

        roleMenuButton.getItems().setAll(bidderItem, sellerItem);
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        String username = usernameField.getText().trim();
        String fullname = fullnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String role = selectedRole;

        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() || role.isEmpty()) {

            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        User newUser = new User(username, fullname, email, password, role);
        DataManager.allUsers.add(newUser);

        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản " + username + " đã được tạo!");
        goToLoginScreen(event);
    }

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