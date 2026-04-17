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


    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;


    public static String userHeThong = "adminUser";
    public static String fullnameHeThong = "Quản trị viên";


    public static String emailHeThong = "";
    public static String passHeThong = "";
    @FXML
    public void goToRegisterScreen(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/Part1/RegisterController.fxml"));
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(registerRoot));

            window.setTitle("Đăng ký tài khoản");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String emailDaNhap = emailField.getText().trim();
        String passDaNhap = passwordField.getText().trim();

        // 1. Kiểm tra rỗng
        if(emailDaNhap.isEmpty() || passDaNhap.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Không được để tài khoản hoặc mật khẩu trống");
            return;
        }

        // 2. Kiểm tra trong danh sách DataManager
        User userHienTai = null;
        boolean timThay = false;

        for (User u : DataManager.allUsers) {
            // So sánh Email và Password (đảm bảo class User đã có hàm getEmail() và getPassword())
            if (u.getEmail().equals(emailDaNhap) && u.getPassword().equals(passDaNhap)) {
                timThay = true;
                userHienTai = u;
                break;
            }
        }

        // 3. Xử lý kết quả đăng nhập
        if (timThay) {
            // Kiểm tra xem có bị Admin chặn không
            if (userHienTai.getStatus().equals("BANNED")) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Tài khoản của bạn đã bị khóa!");
                return;
            }

            try {
                // Chuyển màn hình dựa trên Role (theo yêu cầu bài tập lớn)
                String fxmlPath = "";
                if (userHienTai.getRole().equals("ADMIN")) {
                    fxmlPath = "/Part1/Admin.fxml"; // Nếu là Admin thì vào trang quản lý
                } else {
                    fxmlPath = "/Part1/ProductListController.fxml"; // Bidder/Seller vào trang chủ sản phẩm
                }

                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                Scene scene = new Scene(root);
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(scene);
                window.setTitle("Hệ thống Đấu giá - " + userHienTai.getFullname());
                window.show();

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi đường dẫn FXML!");
            }

        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Sai tài khoản/mật khẩu! Nếu chưa có, vui lòng bấm Register.");
        }
    }

    // Hàm phụ để hiển thị thông báo cho gọn code
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}