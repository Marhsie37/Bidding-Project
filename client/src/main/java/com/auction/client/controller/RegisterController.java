package com.auction.client.controller;

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
        //Tạo 2 MenuItem có tên Bidder và Seller nếu thay Bidder bằng tên khác thì nó sẽ hiện thì cái tên khác và Seller

        bidderItem.setOnAction(e -> {
            selectedRole = "Bidder";
            roleMenuButton.setText("Bidder");
        });
        //Thiết lập hành động khi bấm vào bidder thì sẽ gán tên cho selectedRole để đổi tên cho MenuButton ,đổi tên cho
        //roleMenuButton nhưng lúc này nó chưa chạy luôn

        sellerItem.setOnAction(e -> {
            selectedRole = "Seller";
            roleMenuButton.setText("Seller");
        });

        roleMenuButton.getItems().setAll(bidderItem, sellerItem);
        //getItem() sẽ láy danh sách các lựa chọn bên trong MenuButton lúc này đang trống không
        //và setAll sẽ xóa hết danh sách sẵn có, bỏ bidderItem ,sellerItem vào bên trong
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {
        String username = usernameField.getText().trim();
        String fullname = fullnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        //Các biến đọc văn bản đuọc nhập từ TextFile và bỏ khoảng trắng đầu cuối bằng trim()


        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() || selectedRole.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        //Không bỏ trống thông tin

        if (!email.endsWith("@gmail.com")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Email", "Chỉ chấp nhận tài khoản @gmail.com!");
            return;
        }
        //Tạo tài khoản email gì cũng được nhưng phải kết thúc bằng @gmail.com (!email.endsWith("@gmail.com"))


        if (password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu phải >= 8 ký tự, bao gồm cả chữ và số!");
            return;
        }


        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        for (User u : DataManager.allUsers) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Email này đã được đăng ký!");
                return;
            }
        }
        /* Vì allUsers của DataManager là static nên ta có thể khai báo DataManager.allUsers
        Duyệt từng phần tử bên trong danh sách User của DataManager lấy email bằng getEmail
        và check thử với email vừa nhập vào có trùng mà không phân biệt chữ hoa với chữ thường không
        */


        User newUser = new User(username, fullname, email, password, selectedRole);
        DataManager.allUsers.add(newUser);

        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản " + username + " đã được tạo!");
        goToLoginScreen(event);
        /*Nếu thỏa mãn các điều kiện trên thì sẽ tạo ra User mới và thêm vào biến allUser chung nằm ở DataManager và
        chuyển sang màn hình đăng nhập
        */
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
    //Đoạn code giúp gán onAction cho một vật thể trong Scene để chuyển từ trang Register sang Login
}