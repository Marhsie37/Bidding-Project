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

public class Admin {

    @FXML private VBox vBoxDisplay;
    @FXML private VBox vBoxProducts;
    @FXML private TextField searchField;
    @FXML private Label blTotalActive;

    @FXML
    public void initialize() {
        System.out.println("✅ Admin.initialize() - BẮT ĐẦU");
        loadUsers();
        loadProducts();
    }

    // ==================== USER MANAGEMENT ====================






    // ==================== PRODUCT MANAGEMENT ====================
    private void loadProducts() {
        System.out.println("📦 loadProducts() - GỬI REQUEST");
        Request request = new Request(CommandType.ADMIN_GET_ALL_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            System.out.println("📥 loadProducts() nhận response: success=" + response.isSuccess());
            System.out.println("📥 response.getData(): " + response.getData());

            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    vBoxProducts.getChildren().clear();
                    Object productsData = response.getData().get("products");
                    System.out.println("📥 productsData: " + productsData);
                    System.out.println("📥 productsData type: " + (productsData != null ? productsData.getClass().getName() : "null"));

                    if (productsData instanceof List) {
                        List<?> rawList = (List<?>) productsData;
                        System.out.println("📥 Số lượng product: " + rawList.size());
                        int totalActive = 0;

                        for (Object obj : rawList) {
                            System.out.println("📥 obj type: " + obj.getClass().getName());

                            if (obj instanceof Map) {
                                Map<String, Object> pMap = (Map<String, Object>) obj;
                                System.out.println("📥 pMap: " + pMap);
                                totalActive++;

                                HBox row = new HBox(15);
                                row.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

                                String title = (String) pMap.getOrDefault("name", "Không rõ tên");
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
                                System.out.println("✅ Đã thêm product: " + title);
                            } else if (obj instanceof com.auction.shared.model.Product) {
                                com.auction.shared.model.Product p = (com.auction.shared.model.Product) obj;
                                totalActive++;

                                HBox row = new HBox(15);
                                row.setStyle("-fx-padding: 10; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

                                Label lblName = new Label(p.getName());
                                lblName.setPrefWidth(200);

                                Label lblPrice = new Label(String.format("%,.0f VNĐ", p.getCurrentPrice()));
                                lblPrice.setPrefWidth(120);
                                lblPrice.setStyle("-fx-text-fill: red;");

                                Button btnDelete = new Button("Xóa");
                                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                                btnDelete.setOnAction(e -> deleteProduct(p.getId()));

                                row.getChildren().addAll(lblName, lblPrice, btnDelete);
                                vBoxProducts.getChildren().add(row);
                                System.out.println("✅ Đã thêm product: " + p.getName());
                            } else {
                                System.out.println("❌ Không xử lý được obj type: " + obj.getClass().getName());
                            }
                        }
                        if (blTotalActive != null) {
                            blTotalActive.setText("Tổng sản phẩm đang đấu giá: " + totalActive);
                        }
                    } else {
                        System.out.println("❌ productsData không phải là List!");
                        Label errorLabel = new Label("Dữ liệu sản phẩm không đúng format: " + productsData);
                        vBoxProducts.getChildren().add(errorLabel);
                    }
                } else {
                    System.out.println("❌ loadProducts thất bại!");
                    Label errorLabel = new Label("Không thể tải dữ liệu sản phẩm: " + response.getMessage());
                    vBoxProducts.getChildren().add(errorLabel);
                }
            });
        });
    }

    private void loadUsers() {
        System.out.println("🚀 loadUsers() - GỬI REQUEST");
        Request request = new Request(CommandType.ADMIN_GET_ALL_USERS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            System.out.println("📥 loadUsers() nhận response: success=" + response.isSuccess());

            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    vBoxDisplay.getChildren().clear();
                    Object usersData = response.getData().get("users");

                    if (usersData instanceof List) {
                        List<?> rawList = (List<?>) usersData;

                        for (Object obj : rawList) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/UserAdmin.fxml"));
                                Node row = loader.load();
                                UserItemController controller = loader.getController();

                                if (obj instanceof Map) {
                                    Map<String, Object> userMap = (Map<String, Object>) obj;
                                    String role = (String) userMap.getOrDefault("role", "");
                                    if ("ADMIN".equalsIgnoreCase(role)) continue;

                                    controller.setDataFromMap(userMap);

                                    int userId = ((Number) userMap.get("id")).intValue();
                                    Button btnBan = controller.getBtnBan();
                                    Button btnDelete = controller.getBtnDelete();
                                    Label lblInfo = controller.getLblInfo();

                                    btnBan.setOnAction(e -> toggleBanUser(userId, btnBan, lblInfo));
                                    btnDelete.setOnAction(e -> deleteUser(userId, row));

                                    vBoxDisplay.getChildren().add(row);
                                    System.out.println("✅ Đã thêm user (Map): " + userMap.get("username"));

                                } else if (obj instanceof com.auction.shared.model.User) {
                                    com.auction.shared.model.User user = (com.auction.shared.model.User) obj;
                                    if ("ADMIN".equalsIgnoreCase(user.getRole())) continue;

                                    controller.setData(user);

                                    Button btnBan = controller.getBtnBan();
                                    Button btnDelete = controller.getBtnDelete();
                                    Label lblInfo = controller.getLblInfo();

                                    btnBan.setOnAction(e -> toggleBanUser(user.getId(), btnBan, lblInfo));
                                    btnDelete.setOnAction(e -> deleteUser(user.getId(), row));

                                    vBoxDisplay.getChildren().add(row);
                                    System.out.println("✅ Đã thêm user (Object): " + user.getUsername());

                                } else {
                                    System.out.println("❌ Không xử lý được obj type: " + obj.getClass().getName());
                                }

                            } catch (IOException e) {
                                System.err.println("❌ Lỗi load User FXML: " + e.getMessage());
                            }
                        }
                    } else {
                        System.out.println("❌ usersData không phải là List!");
                        vBoxDisplay.getChildren().add(new Label("Dữ liệu không đúng format"));
                    }
                } else {
                    System.out.println("❌ loadUsers thất bại!");
                    vBoxDisplay.getChildren().add(new Label("Không thể tải dữ liệu: " + response.getMessage()));
                }
            });
        });
    }

    private void toggleBanUser(int userId, Button btnBan, Label lblInfo) {
        btnBan.setDisable(true);

        boolean currentlyBanned = "Unban".equals(btnBan.getText());
        CommandType cmd = currentlyBanned ? CommandType.ADMIN_UNBAN_USER : CommandType.ADMIN_BAN_USER;

        System.out.println("🔘 toggleBanUser - userId=" + userId + " | currentlyBanned=" + currentlyBanned + " | cmd=" + cmd); // THÊM

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(cmd, data);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            System.out.println("📥 toggleBanUser response: success=" + response.isSuccess() + " | msg=" + response.getMessage()); // THÊM
            Platform.runLater(() -> {
                btnBan.setDisable(false);
                if (response.isSuccess()) {
                    if (currentlyBanned) {
                        btnBan.setText("Ban");
                        btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                        if (lblInfo != null) {
                            lblInfo.setText(lblInfo.getText().replace("BANNED", "ACTIVE"));
                        }
                        showAlert("Thành công", "Đã mở khóa người dùng!");
                    } else {
                        btnBan.setText("Unban");
                        btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
                        if (lblInfo != null) {
                            lblInfo.setText(lblInfo.getText().replace("ACTIVE", "BANNED"));
                        }
                        showAlert("Thành công", "Đã khóa người dùng!");
                    }
                } else {
                    showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Thao tác thất bại!");
                }
            });
        });
    }

    // ==================== USER ACTIONS ====================



    private void deleteUser(int userId, Node row) {
        // ✅ Tìm nút delete và disable
        if (row instanceof HBox) {
            ((HBox) row).getChildren().forEach(n -> n.setDisable(true));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Request request = new Request(CommandType.ADMIN_DELETE_USER, data);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    vBoxDisplay.getChildren().remove(row);
                    showAlert("Thành công", "Đã xóa người dùng!");
                } else {
                    if (row instanceof HBox) {
                        ((HBox) row).getChildren().forEach(n -> n.setDisable(false));
                    }
                    showAlert("Lỗi", response.getMessage());
                }
            });
        });
    }

    private void deleteProduct(int productId) {
        // ✅ Cần truyền thêm row để disable, sửa signature
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

    // ==================== NAVIGATION ====================

    @FXML
    public void toLogin(ActionEvent event) {
        System.out.println("🔄 Đang gửi yêu cầu đăng xuất an toàn lên Server...");
        SocketClient.getInstance().logout(response -> {
            Platform.runLater(() -> {
                try {Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    oldStage.close();
                    WindowManager.openUndecoratedWindow("/com/auction/client/view/LoginController.fxml", this);
                    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
                    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    window.setScene(new Scene(root));
                    window.centerOnScreen();
                    window.show();
                    System.out.println("✅ Đã quay về màn hình Đăng nhập an toàn.");
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