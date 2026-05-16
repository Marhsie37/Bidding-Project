package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.Product;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SellingController implements Initializable {

    @FXML private VBox vboxDisplay;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private TextField txtImageUrl;
    @FXML private TextField txtDuration;
    @FXML private TextArea txtDescription;

    private Product selectedProduct = null;
    private Map<Integer, Timeline> timelines = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SellingController initialized");
        loadMyProducts();
    }

    private void loadMyProducts() {
        Request request = new Request(CommandType.GET_MY_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    List<Product> products = (List<Product>) data.get("products");

                    vboxDisplay.getChildren().clear();
                    for (Timeline t : timelines.values()) t.stop();
                    timelines.clear();

                    if (products != null) {
                        for (Product p : products) reconstructProductUI(p);
                    }
                }
            });
        });
    }

    private void reconstructProductUI(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductItem.fxml"));
            HBox productRow = loader.load();

            ProductItemController controller = loader.getController();
            controller.setData(p);

            Label lblTimer = controller.getLblTimer();

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int remaining = p.getRemainingSeconds();
                if (remaining <= 0) {
                    lblTimer.setText("HẾT HẠN!");
                    productRow.setOpacity(0.6);
                } else {
                    lblTimer.setText(formatTime(remaining));
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            timelines.put(p.getId(), timeline);

            controller.getBtnDelete().setOnAction(e -> deleteProduct(p.getId()));

            controller.getBtnEdit().setOnAction(e -> {
                selectedProduct = p;
                txtName.setText(p.getName());
                txtPrice.setText(String.valueOf(p.getCurrentPrice()));
                txtImageUrl.setText(p.getImageUrl());
                txtDuration.setText(String.valueOf(p.getDurationSeconds()));
                txtDescription.setText(p.getDescription());
            });

            vboxDisplay.getChildren().add(productRow);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteProduct(int productId) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.DELETE_PRODUCT, data);
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    System.out.println("✅ Đã xóa sản phẩm!");
                    loadMyProducts();
                } else {
                    System.err.println("❌ Lỗi xóa: " + response.getMessage());
                }
            });
        });
    }

    @FXML
    public void handleAddProduct(ActionEvent event) {
        try {
            String name = txtName.getText().trim();
            double price = Double.parseDouble(txtPrice.getText().trim());
            String imageUrl = txtImageUrl.getText().trim();
            int durationSeconds = txtDuration.getText().isEmpty() ? 86400 : Integer.parseInt(txtDuration.getText().trim());
            String description = txtDescription.getText().trim();

            if (name.isEmpty()) {
                System.out.println("Tên sản phẩm không được để trống!");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("startingPrice", price);
            data.put("imageUrl", imageUrl);
            data.put("durationSeconds", durationSeconds);
            data.put("description", description);
            data.put("category", "Khác");

            if (selectedProduct == null) {
                Request request = new Request(CommandType.ADD_PRODUCT, data);
                SocketClient.getInstance().sendRequestAsync(request, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            System.out.println("✅ Thêm sản phẩm thành công!");
                            clearFields();
                            loadMyProducts();
                        } else {
                            System.err.println("❌ Lỗi thêm: " + response.getMessage());
                        }
                    });
                });
            } else {
                data.put("productId", selectedProduct.getId());
                Request request = new Request(CommandType.UPDATE_PRODUCT, data);
                SocketClient.getInstance().sendRequestAsync(request, response -> {
                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            System.out.println("✅ Cập nhật sản phẩm thành công!");
                            clearFields();
                            selectedProduct = null;
                            loadMyProducts();
                        } else {
                            System.err.println("❌ Lỗi cập nhật: " + response.getMessage());
                        }
                    });
                });
            }
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Giá và thời gian phải là số!");
        }
    }

    private void clearFields() {
        txtName.clear();
        txtPrice.clear();
        txtImageUrl.clear();
        txtDuration.clear();
        txtDescription.clear();
        selectedProduct = null;
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "HẾT HẠN!";
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        String result = "";
        if (days > 0) result += days + " ngày ";
        if (hours > 0 || days > 0) result += hours + " giờ ";
        if (minutes > 0 || hours > 0 || days > 0) result += minutes + " phút ";
        result += seconds + " giây";
        return result;
    }

    @FXML public void toProductListController(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/ProductListController.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }

    @FXML public void toAdmin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Admin.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }

    @FXML public void goToLoginScreen(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }

    @FXML private void toProfile(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Profile.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }
}