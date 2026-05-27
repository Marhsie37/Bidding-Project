package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.Product;
import com.auction.shared.model.User;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSeller {

  private static final Logger logger = LoggerFactory.getLogger(ProfileSeller.class);

  @FXML
  private TextField txtUsername;
  @FXML
  private TextField txtFullName;
  @FXML
  private TextField txtEmail;
  @FXML
  private TextField txtBalance;
  @FXML
  private TextField txtAmount;
  @FXML
  private Button btnRecharge;

  @FXML
  private VBox vBoxActiveProducts;
  @FXML
  private VBox vBoxSoldProducts;

  private User currentUser;
  private final List<Timeline> activeTimelines = new ArrayList<>();

  @FXML
  public void initialize() {

    logger.info("ProfileSeller initialized");
    loadUserInfo();
  }


  private void loadUserInfo() {
    logger.info("🔍 Gửi request GET_USER_INFO...");
    Request request = new Request(CommandType.GET_USER_INFO, new HashMap<>());
    SocketClient.getInstance().sendRequestAsync(request, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess() && response.getData() != null) {
          Map<String, Object> data = response.getData();
          Object userObj = data.get("user");
          if (userObj instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) userObj;
            currentUser = new User();
            currentUser.setId(((Number) userData.get("id")).intValue());
            currentUser.setUsername((String) userData.get("username"));
            currentUser.setFullName((String) userData.get("fullName"));
            currentUser.setEmail((String) userData.get("email"));
            double balance = userData.get("balance") != null
                    ? ((Number) userData.get("balance")).doubleValue() : 0;
            currentUser.setBalance(balance);

            if (txtUsername != null) txtUsername.setText(currentUser.getUsername());
            if (txtFullName != null) txtFullName.setText(currentUser.getFullName());
            if (txtEmail != null) txtEmail.setText(currentUser.getEmail());
            if (txtBalance != null)
              txtBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));

            logger.info("Đã tải thông tin user: {}", currentUser.getUsername());
          } else {
            logger.warn("userObj không phải là Map: {}", userObj);
          }
        } else {
          logger.warn("loadUserInfo thất bại: {}", response.getMessage());
        }
        // Load sản phẩm sau khi tải user info xong
        loadSellerProducts();
      });
    });
  }

  private void loadSellerProducts() {
    logger.info("🔍 Gọi GET_MY_PRODUCTS...");
    Request request = new Request(CommandType.GET_MY_PRODUCTS, new HashMap<>());
    SocketClient.getInstance().sendRequestAsync(request, response -> {
      Platform.runLater(() -> {
        // Dừng tất cả timers cũ
        for (Timeline t : activeTimelines) t.stop();
        activeTimelines.clear();

        if (vBoxActiveProducts != null) vBoxActiveProducts.getChildren().clear();
        if (vBoxSoldProducts != null) vBoxSoldProducts.getChildren().clear();

        if (!response.isSuccess() || response.getData() == null) {
          logger.warn("loadSellerProducts thất bại: {}", response.getMessage());
          addEmptyLabel(vBoxActiveProducts, "Không thể tải danh sách sản phẩm");
          addEmptyLabel(vBoxSoldProducts, "Không thể tải danh sách sản phẩm");
          return;
        }

        Map<String, Object> data = response.getData();
        Object productsObj = data.get("products");
        List<Product> products = null;
        if (productsObj instanceof List) {
          products = (List<Product>) productsObj;
        }

        if (products == null || products.isEmpty()) {
          logger.info("Seller chưa có sản phẩm nào");
          addEmptyLabel(vBoxActiveProducts, "Bạn chưa có sản phẩm nào đang bán");
          addEmptyLabel(vBoxSoldProducts, "Bạn chưa bán được sản phẩm nào");
          return;
        }

        List<Product> activeList = new ArrayList<>();
        List<Product> soldList = new ArrayList<>();

        for (Product p : products) {
          String status = p.getStatus();
          if ("ACTIVE".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
            activeList.add(p);
          } else if ("SOLD".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
            soldList.add(p);
          }
        }

        logger.info("Active: {}, Sold: {}", activeList.size(), soldList.size());

        if (activeList.isEmpty()) {
          addEmptyLabel(vBoxActiveProducts, "Bạn chưa có sản phẩm nào đang bán");
        } else {
          for (Product p : activeList) {
            HBox card = buildActiveProductCard(p);
            if (vBoxActiveProducts != null) vBoxActiveProducts.getChildren().add(card);
          }
        }

        if (soldList.isEmpty()) {
          addEmptyLabel(vBoxSoldProducts, "Bạn chưa bán được sản phẩm nào");
        } else {
          for (Product p : soldList) {
            HBox card = buildSoldProductCard(p);
            if (vBoxSoldProducts != null) vBoxSoldProducts.getChildren().add(card);
          }
        }
      });
    });
  }

  // ─────────────────────────── UI BUILDERS ────────────────────────────

  private HBox buildActiveProductCard(Product p) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ActiveProductItem.fxml"));
      HBox card = loader.load();
      ActiveProductItemController controller = loader.getController();
      controller.setData(p);

      Label lblTimer = controller.getLblTimer();
      updateTimerLabel(lblTimer, p);

      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimerLabel(lblTimer, p)));
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
      activeTimelines.add(timeline);

      return card;
    } catch (Exception e) {
      logger.error("Lỗi khi load ActiveProductItem.fxml: ", e);
      return null;
    }
  }

  private HBox buildSoldProductCard(Product p) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/SoldProductItem.fxml"));
      HBox card = loader.load();
      SoldProductItemController controller = loader.getController();
      controller.setData(p);
      return card;
    } catch (Exception e) {
      logger.error("Lỗi khi load SoldProductItem.fxml: ", e);
      return null;
    }
  }

  private void updateTimerLabel(Label lbl, Product p) {
    if (lbl == null) return;
    int remaining = p.getRemainingSeconds();
    if (remaining <= 0) {
      lbl.setText("⏰ Đã kết thúc");
      lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999; -fx-font-weight: bold;");
    } else {
      lbl.setText("⏱ " + formatTime(remaining));
    }
  }

  private String formatTime(int totalSeconds) {
    if (totalSeconds <= 0) return "Hết hạn";
    int days = totalSeconds / 86400;
    int hours = (totalSeconds % 86400) / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int seconds = totalSeconds % 60;
    if (days > 0) return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
    if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
    return String.format("%02d:%02d", minutes, seconds);
  }

  private void addEmptyLabel(VBox container, String text) {
    if (container == null) return;
    Label lbl = new Label(text);
    lbl.setStyle(
            "-fx-padding: 30 20 30 20;" +
                    "-fx-text-fill: #aaaaaa;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-style: italic;"
    );
    container.getChildren().add(lbl);
  }

  // ─────────────────────────── NAVIGATION ────────────────────────────

  @FXML
  public void goBack(ActionEvent event) {
    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/Selling.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi quay lại màn hình chính: ", e);
      showAlert("Lỗi", "Không thể quay lại màn hình chính!");
    }
  }

  @FXML
  public void goToLoginScreen(ActionEvent event) {
    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi chuyển về màn hình đăng nhập: ", e);
      showAlert("Lỗi", "Không thể quay lại màn hình đăng nhập!");
    }
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  @FXML
  public void goToMyProducts(ActionEvent event) {
    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/Selling.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi quay lại màn hình chính: ", e);
      showAlert("Lỗi", "Không thể quay lại màn hình chính!");
    }
  }
}