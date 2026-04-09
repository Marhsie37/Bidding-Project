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

public class LoginController {


    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;


    public static String userHeThong = "admin_user";
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


        if (emailDaNhap.equals(emailHeThong) && passDaNhap.equals(passHeThong)) {

            try {
                Parent homeRoot = FXMLLoader.load(getClass().getResource("/Part1/Page.fxml"));
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

            alert.setContentText("Sai tài khoản/mật khẩu! Nếu chưa có tài khoản, vui lòng bấm Register để tạo.");
            alert.showAndWait();
        }
    }
}