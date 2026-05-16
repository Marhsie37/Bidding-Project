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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Profile {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBalance;
    @FXML private TextField txtAmount;
    @FXML private Button btnRecharge;
    @FXML private VBox vboxPurchasedProducts;

    private User currentUser;
    private static final Logger logger = LoggerFactory.getLogger(Profile.class);
    @FXML
    public void initialize() {
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
                        currentUser.setId((int) userData.get("id"));
                        currentUser.setUsername((String) userData.get("username"));
                        currentUser.setFullName((String) userData.get("fullName"));
                        currentUser.setEmail((String) userData.get("email"));
                        double balance = userData.get("balance") != null ? ((Number) userData.get("balance")).doubleValue() : 0;
                        currentUser.setBalance(balance);
                        txtUsername.setText(currentUser.getUsername());
                        txtFullName.setText(currentUser.getFullName());
                        txtEmail.setText(currentUser.getEmail());
                        txtBalance.setText(String.format("%,.0f", currentUser.getBalance()) + " VNĐ");
                    }
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
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    List<Product> products = (List<Product>) data.get("products");
                    vboxPurchasedProducts.getChildren().clear();
                    if (products != null && !products.isEmpty()) {
                        for (Product p : products) {
                            Label lbl = new Label(p.getName() + " - " + String.format("%,.0f", p.getCurrentPrice()) + " VNĐ");
                            lbl.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;");
                            vboxPurchasedProducts.getChildren().add(lbl);
                        }
                    } else {
                        Label lbl = new Label("Chưa có sản phẩm nào");
                        vboxPurchasedProducts.getChildren().add(lbl);
                    }
                }
            });
        });
    }

    @FXML
    public void handleRecharge() {
        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            if (amount <= 0) {
                showAlert("Lỗi", "Số tiền phải lớn hơn 0!");
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("amount", amount);
            Request request = new Request(CommandType.RECHARGE_BALANCE, data);
            SocketClient.getInstance().sendRequestAsync(request, response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showAlert("Thành công", "Nạp " + String.format("%,.0f", amount) + " VNĐ thành công!");
                        txtAmount.clear();
                        loadUserInfo();
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/ProductListController.fxml"));
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