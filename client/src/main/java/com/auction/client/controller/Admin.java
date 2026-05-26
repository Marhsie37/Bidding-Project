package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Admin {
  private static final Logger logger = LoggerFactory.getLogger(Admin.class);

  @FXML
  private VBox vBoxDisplay;
  @FXML
  private VBox vBoxProducts;
  @FXML
  private TextField searchField;
  @FXML
  private Label blTotalActive;

  private List<Node> allUserRows = new ArrayList<>();
  private List<Node> allProductRows = new ArrayList<>();

  @FXML
  public void initialize() {
    logger.info("Admin.initialize() - BẮT ĐẦU");

    if (searchField != null) {
      searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        filterUsersAndProducts(newValue);
      });
    }

    loadUsers();
    loadProducts();
  }

  // ==================== USER MANAGEMENT ====================

  private void loadUsers() {
    logger.info("loadUsers() - GỬI REQUEST");
    Request request = new Request(CommandType.ADMIN_GET_ALL_USERS, new HashMap<>());
    SocketClient.getInstance().sendRequestAsync(request, response -> {
      logger.info("loadUsers() nhận response: success={}", response.isSuccess());

      Platform.runLater(() -> {
        if (response.isSuccess() && response.getData() != null) {
          vBoxDisplay.getChildren().clear();
          allUserRows.clear();

          Object usersData = response.getData().get("users");

          if (usersData instanceof List) {
            List<?> rawList = (List<?>) usersData;
            logger.info("Số lượng user: {}", rawList.size());

            for (Object obj : rawList) {
              try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/auction/client/view/UserAdmin.fxml"));
                Node row = loader.load();
                UserItemController controller = loader.getController();
                int userId = 0;

                if (obj instanceof Map) {
                  Map<String, Object> userMap = (Map<String, Object>) obj;
                  String role = (String) userMap.getOrDefault("role", "");
                  if ("ADMIN".equalsIgnoreCase(role))
                    continue;
                  controller.setDataFromMap(userMap);
                  userId = ((Number) userMap.get("id")).intValue();

                } else if (obj instanceof com.auction.shared.model.User) {
                  com.auction.shared.model.User user = (com.auction.shared.model.User) obj;
                  if ("ADMIN".equalsIgnoreCase(user.getRole()))
                    continue;
                  controller.setData(user);
                  userId = user.getId();

                } else {
                  logger.warn("Không xử lý được obj type: {}", obj.getClass().getName());
                  continue;
                }

                Button btnBan = controller.getBtnBan();
                Button btnDelete = controller.getBtnDelete();
                Label lblInfo = controller.getLblInfo();

                final int finalUserId = userId;
                btnBan.setOnAction(e -> toggleBanUser(finalUserId, btnBan, lblInfo));
                btnDelete.setOnAction(e -> deleteUser(finalUserId, row));

                vBoxDisplay.getChildren().add(row);
                allUserRows.add(row);
                logger.info("Đã thêm user");

              } catch (IOException e) {
                logger.error("Lỗi load User FXML: ", e);
              }
            }
          } else {
            logger.warn("usersData không phải là List!");
            Label errorLabel = new Label("Dữ liệu không đúng format");
            vBoxDisplay.getChildren().add(errorLabel);
            allUserRows.add(errorLabel);
          }
        } else {
          logger.warn("loadUsers thất bại!");
          Label errorLabel = new Label("Không thể tải dữ liệu: " + response.getMessage());
          vBoxDisplay.getChildren().add(errorLabel);
          allUserRows.add(errorLabel);
        }
      });
    });
  }

  // ==================== PRODUCT MANAGEMENT ====================

  private void loadProducts() {
    logger.info("loadProducts() - GỬI REQUEST");
    Request request = new Request(CommandType.ADMIN_GET_ALL_PRODUCTS, new HashMap<>());
    SocketClient.getInstance().sendRequestAsync(request, response -> {
      logger.info("loadProducts() nhận response: success={}", response.isSuccess());

      Platform.runLater(() -> {
        if (response.isSuccess() && response.getData() != null) {
          vBoxProducts.getChildren().clear();
          allProductRows.clear();

          Object productsData = response.getData().get("products");

          if (productsData instanceof List) {
            List<?> rawList = (List<?>) productsData;
            logger.info("Số lượng product: {}", rawList.size());
            int totalActive = 0;

            for (Object obj : rawList) {
              String productName = "Không rõ tên";
              double currentPrice = 0;
              int productId = 0;

              if (obj instanceof Map) {
                Map<String, Object> pMap = (Map<String, Object>) obj;
                productName = (String) pMap.getOrDefault("name", "Không rõ tên");
                if (pMap.get("currentPrice") != null) {
                  currentPrice = ((Number) pMap.get("currentPrice")).doubleValue();
                }
                if (pMap.get("id") != null) {
                  productId = ((Number) pMap.get("id")).intValue();
                }

              } else if (obj instanceof com.auction.shared.model.Product) {
                com.auction.shared.model.Product p = (com.auction.shared.model.Product) obj;
                productName = p.getName();
                currentPrice = p.getCurrentPrice();
                productId = p.getId();

              } else {
                logger.warn("Không xử lý được obj type: {}", obj.getClass().getName());
                continue;
              }

              totalActive++;

              HBox row = new HBox(15);
              row.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

              Label lblName = new Label(productName);
              lblName.setPrefWidth(200);

              Label lblPrice = new Label(String.format("%,.0f VNĐ", currentPrice));
              lblPrice.setPrefWidth(120);
              lblPrice.setStyle("-fx-text-fill: red;");

              Button btnDelete = new Button("Xóa");
              btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
              final int finalProductId = productId;
              final HBox finalRow = row;
              btnDelete.setOnAction(e -> deleteProduct(finalProductId, finalRow));

              row.getChildren().addAll(lblName, lblPrice, btnDelete);
              vBoxProducts.getChildren().add(row);
              allProductRows.add(row);
              logger.info("Đã thêm product: {}", productName);
            }

            if (blTotalActive != null) {
              blTotalActive.setText("Tổng sản phẩm đang đấu giá: " + totalActive);
            }
          } else {
            logger.warn("productsData không phải là List!");
            Label errorLabel = new Label("Dữ liệu sản phẩm không đúng format");
            vBoxProducts.getChildren().add(errorLabel);
            allProductRows.add(errorLabel);
          }
        } else {
          logger.warn("loadProducts thất bại!");
          Label errorLabel = new Label("Không thể tải dữ liệu sản phẩm: " + response.getMessage());
          vBoxProducts.getChildren().add(errorLabel);
          allProductRows.add(errorLabel);
        }
      });
    });
  }

  // ==================== FILTER ====================

  private void filterUsersAndProducts(String keyword) {
    if (keyword == null)
      keyword = "";
    String lowerKeyword = keyword.trim().toLowerCase();

    if (lowerKeyword.isEmpty()) {
      vBoxDisplay.getChildren().setAll(allUserRows);
      vBoxProducts.getChildren().setAll(allProductRows);
      return;
    }

    List<Node> filteredUsers = new ArrayList<>();
    for (Node row : allUserRows) {
      if (matchesUserKeyword(row, lowerKeyword)) {
        filteredUsers.add(row);
      }
    }
    vBoxDisplay.getChildren().setAll(filteredUsers);

    List<Node> filteredProducts = new ArrayList<>();
    for (Node row : allProductRows) {
      if (matchesProductKeyword(row, lowerKeyword)) {
        filteredProducts.add(row);
      }
    }
    vBoxProducts.getChildren().setAll(filteredProducts);
  }

  private boolean matchesUserKeyword(Node row, String keyword) {
    if (row instanceof HBox) {
      for (Node child : ((HBox) row).getChildren()) {
        if (child instanceof Label) {
          Label lbl = (Label) child;
          if (lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword))
            return true;
        } else if (child instanceof VBox) {
          for (Node inner : ((VBox) child).getChildren()) {
            if (inner instanceof Label) {
              Label lbl = (Label) inner;
              if (lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword))
                return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean matchesProductKeyword(Node row, String keyword) {
    if (row instanceof HBox) {
      for (Node child : ((HBox) row).getChildren()) {
        if (child instanceof Label) {
          Label lbl = (Label) child;
          if (lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword))
            return true;
        }
      }
    }
    return false;
  }

  // ==================== USER ACTIONS ====================

  private void toggleBanUser(int userId, Button btnBan, Label lblInfo) {
    btnBan.setDisable(true);

    boolean currentlyBanned = "Unban".equals(btnBan.getText());
    CommandType cmd = currentlyBanned ? CommandType.ADMIN_UNBAN_USER : CommandType.ADMIN_BAN_USER;
    logger.info("toggleBanUser - userId={} | currentlyBanned={}", userId, currentlyBanned);

    Map<String, Object> data = new HashMap<>();
    data.put("userId", userId);
    Request request = new Request(cmd, data);

    SocketClient.getInstance().sendRequestAsync(request, response -> {
      logger.info("toggleBanUser response: success={}", response.isSuccess());
      Platform.runLater(() -> {
        btnBan.setDisable(false);
        if (response.isSuccess()) {
          if (currentlyBanned) {
            btnBan.setText("Ban");
            btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
            if (lblInfo != null && lblInfo.getText().contains("BANNED")) {
              lblInfo.setText(lblInfo.getText().replace("BANNED", "ACTIVE"));
            }
            showAlert("Thành công", "Đã mở khóa người dùng!");
          } else {
            btnBan.setText("Unban");
            btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
            if (lblInfo != null && lblInfo.getText().contains("ACTIVE")) {
              lblInfo.setText(lblInfo.getText().replace("ACTIVE", "BANNED"));
            }
            showAlert("Thành công", "Đã khóa người dùng!");
          }
          loadUsers();
        } else {
          showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Thao tác thất bại!");
        }
      });
    });
  }

  private void deleteUser(int userId, Node row) {
    if (row instanceof HBox) {
      ((HBox) row).setDisable(true);
    }

    Map<String, Object> data = new HashMap<>();
    data.put("userId", userId);
    Request request = new Request(CommandType.ADMIN_DELETE_USER, data);

    SocketClient.getInstance().sendRequestAsync(request, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess()) {
          vBoxDisplay.getChildren().remove(row);
          allUserRows.remove(row);
          showAlert("Thành công", "Đã xóa người dùng!");
        } else {
          if (row instanceof HBox) {
            ((HBox) row).setDisable(false);
          }
          showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Xóa thất bại!");
        }
      });
    });
  }

  private void deleteProduct(int productId, HBox row) {
    row.setDisable(true);

    Map<String, Object> data = new HashMap<>();
    data.put("productId", productId);
    Request request = new Request(CommandType.ADMIN_DELETE_PRODUCT, data);

    SocketClient.getInstance().sendRequestAsync(request, response -> {
      Platform.runLater(() -> {
        if (response.isSuccess()) {
          vBoxProducts.getChildren().remove(row);
          allProductRows.remove(row);

          long remaining = allProductRows.stream().filter(n -> n instanceof HBox).count();
          if (blTotalActive != null) {
            blTotalActive.setText("Tổng sản phẩm đang đấu giá: " + remaining);
          }
          showAlert("Thành công", "Đã xóa sản phẩm thành công!");
        } else {
          row.setDisable(false);
          showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Xóa thất bại!");
        }
      });
    });
  }

  // ==================== NAVIGATION ====================

  @FXML
  public void toLogin(ActionEvent event) {
    logger.info("Đang gửi yêu cầu đăng xuất...");
    SocketClient.getInstance().logout(response -> {
      Platform.runLater(() -> {
        try {
          Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
          oldStage.close();
          WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", this);
        } catch (Exception e) {
          logger.error("Lỗi khi chuyển về màn hình Login: ", e);
        }
      });
    });
  }

  @FXML
  public void toSelling(ActionEvent event) {
    try {
      Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      oldStage.close();
      WindowManager.openWindow("/com/auction/client/view/Selling.fxml", this);
    } catch (Exception e) {
      logger.error("Lỗi khi chuyển sang màn hình Selling: ", e);
    }
  }

  // ==================== UTILITY ====================

  private void showAlert(String title, String content) {
    Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
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
}