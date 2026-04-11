package Part1;

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

    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;


    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private TextField confirmPasswordField;

    @FXML
    public void goToLoginScreen(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Part1/LoginController.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.setTitle("Đăng nhập");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCreateAccount(ActionEvent event) {

        String username = usernameField.getText().trim();
        String fullname = fullnameField.getText().trim();

        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();


        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }


        boolean isSaved = saveUserToDatabase(username, fullname, email, password);

        if (isSaved) {

            Part1.LoginController.userHeThong = username;
            Part1.LoginController.fullnameHeThong = fullname;


            Part1.LoginController.emailHeThong = email;
            Part1.LoginController.passHeThong = password;

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo tài khoản thành công! Nhấn OK để quay lại trang Đăng nhập.");
            goToLoginScreen(event);
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Email này đã được sử dụng. Vui lòng chọn email khác!");
        }
    }


    private boolean saveUserToDatabase(String username, String fullname, String email, String password) {
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