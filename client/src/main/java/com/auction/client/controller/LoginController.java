package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class LoginController {
  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

  @FXML
  private TextField userNameField;
  @FXML
  private TextField passwordField;

  @FXML
  public void handleLogin(ActionEvent event) {
    String username = userNameField.getText().trim();
    String password = passwordField.getText().trim();

    if (username.isEmpty() || password.isEmpty()) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
      return;
    }

    SocketClient socketClient = SocketClient.getInstance();

    // ✅ Luôn disconnect cũ và connect mới để tránh dùng socket đã chết
    if (socketClient.isConnected()) {
      logger.info("🔌 Đang ngắt kết nối cũ...");
      socketClient.disconnect();
    }

    try {
      logger.info("🔌 Đang kết nối đến server...");
      socketClient.connect();
      logger.info("✅ Đã kết nối server");
    } catch (IOException e) {
      logger.error("❌ Lỗi kết nối: ", e);
      showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage());
      return;
    }

    socketClient.login(username, password, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess()) {
          handleLoginSuccess(event, response);
        } else {
          showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", response.getMessage());
        }
      });
    });
  }

  @SuppressWarnings("unchecked")
  private void handleLoginSuccess(ActionEvent event, Response response) {
    try {
      Map<String, Object> data = (Map<String, Object>) response.getData();
      logger.info("📦 TOÀN BỘ DATA TỪ SERVER: {}", data);

      String role = "";
      if (data != null) {
        // ✅ Kiểm tra lấy role từ các cấu trúc dữ liệu bọc của gói tin trả về
        if (data.containsKey("role")) {
          role = data.get("role").toString();
        } else if (data.containsKey("userData") && data.get("userData") instanceof Map) {
          Map<String, Object> userData = (Map<String, Object>) data.get("userData");
          if (userData.containsKey("role")) {
            role = userData.get("role").toString();
          }
        } else if (data.containsKey("user") && data.get("user") instanceof Map) {
          Map<String, Object> user = (Map<String, Object>) data.get("user");
          if (user.containsKey("role")) {
            role = user.get("role").toString();
          }
        }
      }

      role = role.trim().toUpperCase();
      logger.info("🔍 ROLE LẤY ĐƯỢC: [{}]", role);

      String fxmlPath;
      switch (role) {
        case "ADMIN":
          fxmlPath = "/com/auction/client/view/Admin.fxml";
          break;
        case "SELLER":
          fxmlPath = "/com/auction/client/view/Selling.fxml";
          break;
        case "BIDDER":
          fxmlPath = "/com/auction/client/view/Bidder.fxml";
          break;
        default:
          fxmlPath = "/com/auction/client/view/Bidder.fxml";
          logger.warn("⚠️ Role không xác định: {}, chuyển về màn hình mặc định", role);
          break;
      }

      // ✅ Đóng cửa sổ đăng nhập cũ và mở giao diện phân quyền tương ứng
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow(fxmlPath, this);
      MainApp.initNotificationManager();

    } catch (Exception e) {
      logger.error("Lỗi khi xử lý đăng nhập thành công: ", e);
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện: " + e.getMessage());
    }
  }

  @FXML
  public void goToRegisterScreen(ActionEvent event) {
    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/RegisterController.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi chuyển sang màn hình đăng ký: ", e);
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng ký!");
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}