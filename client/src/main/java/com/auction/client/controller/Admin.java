package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Admin {
    private static final Logger logger = LoggerFactory.getLogger(Admin.class);
    @FXML private VBox vBoxDisplay;
    @FXML private VBox vBoxProducts;
    @FXML private TextField searchField;
    @FXML private Label blTotalActive;

    @FXML
    public void initialize() {
        logger.info("✅ Admin.initialize() - BẮT ĐẦU");
        loadUsers();
        loadProducts();
    }

    // ==================== USER MANAGEMENT ====================\n
    private void loadUsers() {
        logger.info("🚀 loadUsers() - GỬI REQUEST");
        Request request = new Request(CommandType.ADMIN_GET_ALL_USERS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                logger.info("📥 loadUsers() nhận response: success={}", response.isSuccess());                if (response.isSuccess() && response.getData() != null) {
                    vBoxDisplay.getChildren().clear();
                    Object usersData = response.getData().get("users");

                    if (usersData instanceof List) {
                        List<?> rawList = (List<?>) usersData;
                        for (Object obj : rawList) {
                            if (obj instanceof Map) {
                                Map<String, Object> userMap = (Map<String, Object>) obj;

                                String role = (String) userMap.getOrDefault("role", "");
                                if ("ADMIN".equalsIgnoreCase(role)) continue;

                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/UserAdmin.fxml"));
                                    Node row = loader.load();
                                    UserItemController controller = loader.getController();

                                    // ✅ THÊM DÒNG NÀY - set dữ liệu cho controller
                                    controller.setDataFromMap(userMap);

                                    // Gán sự kiện cho nút Ban và Xóa
                                    Button btnBan = controller.getBtnBan();
                                    Button btnDelete = controller.getBtnDelete();

                                    int userId = ((Number) userMap.get("id")).intValue();
                                    btnBan.setOnAction(e -> toggleBanUser(userId, btnBan));
                                    btnDelete.setOnAction(e -> deleteUser(userId, row));

                                    vBoxDisplay.getChildren().add(row);
                                } catch (IOException e) {
                                    logger.error("❌ Lỗi load User FXML: " , e);
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    // ==================== PRODUCT MANAGEMENT ====================\n
    private void loadProducts() {
        logger.info("📦 loadProducts() - GỬI REQUEST");
        Request request = new Request(CommandType.ADMIN_GET_ALL_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                logger.info("📥 loadProducts() nhận response: success={}" , response.isSuccess());
                if (response.isSuccess() && response.getData() != null) {
                    vBoxProducts.getChildren().clear();
                    Object productsData = response.getData().get("products");

                    if (productsData instanceof List) {
                        List<?> rawList = (List<?>) productsData;
                        int totalActive = 0;

                        for (Object obj : rawList) {
                            if (obj instanceof Map) {
                                Map<String, Object> pMap = (Map<String, Object>) obj;
                                totalActive++;

                                HBox row = new HBox(15);
                                row.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

                                String title = (String) pMap.getOrDefault("title", "Không rõ tên");
                                Label lblName = new Label(title);
                                lblName.setPrefWidth(200);

                                double currentPrice = 0.0;
                                if (pMap.get("currentPrice") != null) {
                                    currentPrice = ((Number) pMap.get("currentPrice")).doubleValue();
                                }
                                Label lblPrice = new Label(String.format("%,.0f VNĐ", currentPrice));
                                lblPrice.setPrefWidth(120);
                                lblPrice.setStyle("-fx-text-fill: red;");

                                int productId = ((Number) pMap.get("id")).intValue();
                                Button btnDelete = new Button("Xóa");
                                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                                btnDelete.setOnAction(e -> deleteProduct(productId));

                                row.getChildren().addAll(lblName, lblPrice, btnDelete);
                                vBoxProducts.getChildren().add(row);
                            }
                        }
                        if (blTotalActive != null) {
                            blTotalActive.setText("Tổng sản phẩm đang đấu giá: " + totalActive);
                        }
                    }
                }
            });
        });
    }

    private void deleteProduct(int productId) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.ADMIN_DELETE_PRODUCT, data);
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    loadProducts();
                    showAlert("Thành công", "Đã xóa sản phẩm thành công!");
                } else {
                    showAlert("Lỗi", response.getMessage());
                }
            });
        });
    }

    // ==================== USER ACTIONS ====================

    private void toggleBanUser(int userId, Button btnBan) {
        // Lấy trạng thái hiện tại từ text của nút
        boolean isBanned = btnBan.getText().equals("Unban");
        CommandType cmd = isBanned ? CommandType.ADMIN_UNBAN_USER : CommandType.ADMIN_BAN_USER;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(cmd, data);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    if (isBanned) {
                        btnBan.setText("Ban");
                        btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        showAlert("Thành công", "Đã mở khóa người dùng!");
                    } else {
                        btnBan.setText("Unban");
                        btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        showAlert("Thành công", "Đã khóa người dùng!");
                    }
                    // Refresh lại danh sách để cập nhật status
                    loadUsers();
                } else {
                    showAlert("Lỗi", response.getMessage());
                }
            });
        });
    }

    private void deleteUser(int userId, Node row) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(CommandType.ADMIN_DELETE_USER, data);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    vBoxDisplay.getChildren().remove(row);
                    showAlert("Thành công", "Đã xóa người dùng!");
                } else {
                    showAlert("Lỗi", response.getMessage());
                }
            });
        });
    }

    // ==================== NAVIGATION (SỬA LỖI ĐĂNG XUẤT SẬP SERVER) ====================\n
    @FXML
    public void toLogin(ActionEvent event) {
        logger.info("🔄 Đang gửi yêu cầu đăng xuất an toàn lên Server...");
        // Gọi hàm logout gửi request lên Server xóa session trước khi đổi màn hình
        SocketClient.getInstance().logout(response -> {
            Platform.runLater(() -> {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
                    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    window.setScene(new Scene(root));
                    window.centerOnScreen();
                    window.show();
                    logger.info("✅ Đã quay về màn hình Đăng nhập an toàn.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @FXML
    public void toSelling(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Selling.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}