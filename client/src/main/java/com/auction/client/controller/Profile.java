package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.Product;
import com.auction.shared.model.User;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profile {

    private static final Logger logger = LoggerFactory.getLogger(Profile.class);

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBalance;
    @FXML private TextField txtAmount;
    @FXML private Button btnRecharge;
    @FXML private VBox vboxPurchasedProducts;

    private User currentUser;

    @FXML
    public void initialize() {
        logger.info("Profile initialized");
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
                        double balance = userData.get("balance") != null ? ((Number) userData.get("balance")).doubleValue() : 0;
                        currentUser.setBalance(balance);

                        txtUsername.setText(currentUser.getUsername());
                        txtFullName.setText(currentUser.getFullName());
                        txtEmail.setText(currentUser.getEmail());
                        txtBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));

                        logger.info("Đã tải thông tin user: {}", currentUser.getUsername());
                    } else {
                        logger.warn("userObj không phải là Map: {}", userObj);
                    }
                } else {
                    logger.warn("loadUserInfo thất bại: {}", response.getMessage());
                }
                loadPurchasedProducts();
            });
        });
    }

    private void loadPurchasedProducts() {
        logger.info("🔍 Gọi GET_PURCHASED_PRODUCTS...");
        Request request = new Request(CommandType.GET_PURCHASED_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                vboxPurchasedProducts.getChildren().clear();

                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    Object productsObj = data.get("products");

                    List<Product> products = null;
                    if (productsObj instanceof List) {
                        products = (List<Product>) productsObj;
                    }

                    if (products != null && !products.isEmpty()) {
                        logger.info("Đã tải {} sản phẩm đã mua", products.size());
                        for (Product p : products) {
                            Label lbl = new Label(p.getName() + " - " + String.format("%,.0f VNĐ", p.getCurrentPrice()));
                            lbl.setStyle("-fx-padding: 8; -fx-background-color: #f5f5f5; -fx-background-radius: 5;");
                            vboxPurchasedProducts.getChildren().add(lbl);
                        }
                    } else {
                        Label lbl = new Label("Chưa có sản phẩm nào");
                        lbl.setStyle("-fx-padding: 20; -fx-text-fill: gray;");
                        vboxPurchasedProducts.getChildren().add(lbl);
                        logger.info("Không có sản phẩm đã mua");
                    }
                } else {
                    logger.warn("loadPurchasedProducts thất bại: {}", response.getMessage());
                    Label lbl = new Label("Không thể tải danh sách sản phẩm");
                    lbl.setStyle("-fx-padding: 20; -fx-text-fill: red;");
                    vboxPurchasedProducts.getChildren().add(lbl);
                }
            });
        });
    }

    @FXML
    public void handleRecharge() {
        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            if (amount <= 0) {
                showAlert("Lỗi", "Số tiền nạp phải lớn hơn 0!");
                return;
            }

            // Kiểm tra số tiền hợp lý (tối đa 100 triệu)
            if (amount > 100_000_000) {
                showAlert("Lỗi", "Số tiền nạp tối đa là 100,000,000 VNĐ!");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("amount", amount);
            // ✅ SỬA: dùng ADD_FUNDS thay vì RECHARGE_BALANCE (nếu server dùng ADD_FUNDS)
            Request request = new Request(CommandType.ADD_FUNDS, data);

            btnRecharge.setDisable(true);

            SocketClient.getInstance().sendRequestAsync(request, response -> {
                Platform.runLater(() -> {
                    btnRecharge.setDisable(false);
                    if (response.isSuccess()) {
                        showAlert("Thành công", "Nạp " + String.format("%,.0f", amount) + " VNĐ thành công!");
                        txtAmount.clear();
                        loadUserInfo(); // Refresh thông tin
                        logger.info("Nạp tiền thành công: {} VNĐ", amount);
                    } else {
                        showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Nạp tiền thất bại!");
                        logger.error("Nạp tiền thất bại: {}", response.getMessage());
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void goBack(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/ProductListController.fxml", this);
        } catch (Exception e) {
            logger.error("Lỗi khi quay lại màn hình chính: ", e);
            showAlert("Lỗi", "Không thể quay lại màn hình chính!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}