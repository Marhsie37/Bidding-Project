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

public class LoginController {

    // 1. Khai báo 2 biến này để lấy chữ người dùng gõ vào (tên phải khớp fx:id)
    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;

    public static String emailHeThong = "admin";
    public static String passHeThong = "123";

    // Nút chuyển sang màn hình Đăng ký
    @FXML
    public void goToRegisterScreen(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/Part1/Name.fxml"));
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(registerRoot));
            window.setTitle("Nhập Họ và Tên");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Nút Confirm: Xử lý Đăng nhập
    @FXML
    public void handleLogin(ActionEvent event) {
        String emailDaNhap = emailField.getText().trim();
        String passDaNhap = passwordField.getText().trim();

        // 2. SO SÁNH VỚI BỘ NHỚ TẠM
        if (emailDaNhap.equals(emailHeThong) && passDaNhap.equals(passHeThong)) {

            try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/Part2/Page.fxml"));
                Scene homeScene = new Scene(homeRoot);
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

                window.setScene(homeScene);
                window.setTitle("Trang chủ Sản Phẩm");
                window.show();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi không tải được Page.fxml, hãy kiểm tra lại đường dẫn!");
            }

        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi đăng nhập");
            alert.setHeaderText(null);
            alert.setContentText("Sai tài khoản hoặc mật khẩu!");
            alert.showAndWait();
        }
    }
}