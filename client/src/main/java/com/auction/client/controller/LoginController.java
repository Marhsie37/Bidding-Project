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

    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    String fxmlPath;

    @FXML
    public void handleLogin(ActionEvent event) {
        String emailEntered = emailField.getText().trim();
        String passEntered = passwordField.getText().trim();
        //Gán biến bằng dữ liệu nhận được từ việc nhập văn bản vào TextFile bỏ khoảng trắng

        if(emailEntered.isEmpty() || passEntered.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Không được để trống tài khoản hoặc mật khẩu");
            return;
        }
        //Không được để email và pass trống

        User userCurrent = null;
        for (User user : DataManager.allUsers) {
            if (user.getEmail().equalsIgnoreCase(emailEntered) && user.getPassword().equals(passEntered)) {
                userCurrent = user;
                break;
            }
        }
        //Check coi email và mật khẩu đăng nhập ở Login có xuất hiện ở trong Data Manager không nếu

        if (userCurrent != null) {
            if (userCurrent.getStatus().equals("BANNED")) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Tài khoản của bạn đã bị khóa!");
                return;
            }
            //Check thử trạng thái User hiện tại có bị BANNED không

            try {

                if (userCurrent.getRole().equals("Admin")) {
                    fxmlPath = "/Part1/Admin.fxml";
                } else if(userCurrent.getRole().equals("Bidder")) {
                    fxmlPath = "/Part1/ProductListController.fxml";
                } else if(userCurrent.getRole().equals("Seller")) {
                    fxmlPath = "/Part1/Selling.fxml";
                }
                /*Nếu vai trò là Admin thì lấy đường dẫn của riêng Admin ,nếu có vai trò Bidder hay Seller thì lấy đường dẫn
                sang trang Seller hoặc Bidder

                * */

                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                Stage window =  (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(new Scene(root));
                window.show();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện!");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    public void goToRegisterScreen(ActionEvent event) {
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/Part1/RegisterController.fxml"));
            //Tải file giao diện của màn hình đăng kí
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            //Lấy thông tin của sổ hiện tại từ sự kiện click chuột
            window.setScene(new Scene(registerRoot));
            //set màn hình window bằng màn hình đăng kí
            window.setTitle("Đăng ký tài khoản"); //Đổi tên tiêu đề
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    //Cấu trúc của phần báo lỗi,cảnh báo,....
}