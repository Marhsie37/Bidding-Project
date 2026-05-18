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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class SellingController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SellingController.class);

    @FXML private VBox vboxDisplay;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private TextField txtImageUrl;
    @FXML private TextField txtDuration;
    @FXML private TextArea txtDescription;
    @FXML private TextField searchField;

    private Product selectedProduct = null;
    private Map<Integer, Timeline> timelines = new HashMap<>();
    private List<Node> allProductRows = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("SellingController initialized");

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterProducts(newValue);
            });
        }

        loadMyProducts();
    }

    private void loadMyProducts() {
        logger.info("Loading my products...");
        Request request = new Request(CommandType.GET_MY_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    Object productsObj = data.get("products");

                    List<Product> products = null;
                    if (productsObj instanceof List) {
                        products = (List<Product>) productsObj;
                    }

                    vboxDisplay.getChildren().clear();
                    allProductRows.clear();

                    for (Timeline t : timelines.values()) {
                        t.stop();
                    }
                    timelines.clear();

                    if (products != null && !products.isEmpty()) {
                        logger.info("Found {} products", products.size());
                        for (Product p : products) {
                            reconstructProductUI(p);
                        }
                    } else {
                        logger.info("No products found");
                        Label emptyLabel = new Label("Bạn chưa có sản phẩm nào");
                        emptyLabel.setStyle("-fx-padding: 20; -fx-text-fill: gray;");
                        vboxDisplay.getChildren().add(emptyLabel);
                        allProductRows.add(emptyLabel);
                    }
                } else {
                    logger.warn("Failed to load products: {}", response.getMessage());
                    Label errorLabel = new Label("Không thể tải sản phẩm: " + response.getMessage());
                    errorLabel.setStyle("-fx-padding: 20; -fx-text-fill: red;");
                    vboxDisplay.getChildren().add(errorLabel);
                    allProductRows.add(errorLabel);
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
                if (p == null) return;
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

            Button btnDelete = controller.getBtnDelete();
            btnDelete.setOnAction(e -> deleteProduct(p.getId(), productRow));

            Button btnEdit = controller.getBtnEdit();
            btnEdit.setOnAction(e -> {
                selectedProduct = p;
                txtName.setText(p.getName());
                txtPrice.setText(String.valueOf(p.getCurrentPrice()));
                txtImageUrl.setText(p.getImageUrl());
                txtDuration.setText(String.valueOf(p.getDurationHours()));
                txtDescription.setText(p.getDescription());
                logger.info("Editing product: {}", p.getName());
            });

            vboxDisplay.getChildren().add(productRow);
            allProductRows.add(productRow);
            logger.debug("Added product UI: {}", p.getName());

        } catch (IOException e) {
            logger.error("Error loading ProductItem FXML: ", e);
        }
    }

    private void filterProducts(String keyword) {
        if (keyword == null) {
            keyword = "";
        }
        String lowerKeyword = keyword.trim().toLowerCase();

        if (lowerKeyword.isEmpty()) {
            vboxDisplay.getChildren().setAll(allProductRows);
            return;
        }

        List<Node> filtered = new ArrayList<>();
        for (Node row : allProductRows) {
            if (matchesKeyword(row, lowerKeyword)) {
                filtered.add(row);
            }
        }
        vboxDisplay.getChildren().setAll(filtered);
    }

    private boolean matchesKeyword(Node row, String keyword) {
        if (row instanceof HBox) {
            HBox hbox = (HBox) row;
            for (Node child : hbox.getChildren()) {
                if (child instanceof VBox) {
                    VBox vbox = (VBox) child;
                    for (Node inner : vbox.getChildren()) {
                        if (inner instanceof Label) {
                            Label lbl = (Label) inner;
                            if (lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword)) {
                                return true;
                            }
                        }
                    }
                } else if (child instanceof Label) {
                    Label lbl = (Label) child;
                    if (lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword)) {
                        return true;
                    }
                }
            }
        } else if (row instanceof Label) {
            Label lbl = (Label) row;
            return lbl.getText() != null && lbl.getText().toLowerCase().contains(keyword);
        }
        return false;
    }

    private void deleteProduct(int productId, HBox productRow) {
        productRow.setDisable(true);

        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        Request request = new Request(CommandType.DELETE_PRODUCT, data);

        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                productRow.setDisable(false);
                if (response.isSuccess()) {
                    logger.info("Đã xóa sản phẩm ID: {}", productId);
                    loadMyProducts();
                    showAlert("Thành công", "Đã xóa sản phẩm thành công!");

                    if (selectedProduct != null && selectedProduct.getId() == productId) {
                        clearFields();
                    }
                } else {
                    logger.error("Lỗi xóa: {}", response.getMessage());
                    showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Xóa thất bại!");
                }
            });
        });
    }

    @FXML
    public void handleAddProduct(ActionEvent event) {
        try {
            String name = txtName.getText().trim();
            String priceText = txtPrice.getText().trim();
            String imageUrl = txtImageUrl.getText().trim();
            String durationText = txtDuration.getText().trim();
            String description = txtDescription.getText().trim();

            if (name.isEmpty()) {
                showAlert("Lỗi", "Tên sản phẩm không được để trống!");
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceText);
                if (price <= 0) {
                    showAlert("Lỗi", "Giá khởi điểm phải lớn hơn 0!");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Lỗi", "Giá không hợp lệ!");
                return;
            }

            int durationHours;
            try {
                durationHours = durationText.isEmpty() ? 24 : Integer.parseInt(durationText);
                if (durationHours <= 0) {
                    showAlert("Lỗi", "Thời gian đấu giá phải lớn hơn 0!");
                    return;
                }
                if (durationHours > 720) {
                    showAlert("Lỗi", "Thời gian đấu giá tối đa là 720 giờ (30 ngày)!");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Lỗi", "Thời gian không hợp lệ!");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("startingPrice", price);
            data.put("imageUrl", imageUrl);
            data.put("durationHours", durationHours);
            data.put("description", description);
            data.put("category", "Khác");

            Button sourceButton = (Button) event.getSource();
            sourceButton.setDisable(true);

            if (selectedProduct == null) {
                Request request = new Request(CommandType.ADD_PRODUCT, data);
                SocketClient.getInstance().sendRequestAsync(request, response -> {
                    Platform.runLater(() -> {
                        sourceButton.setDisable(false);
                        if (response.isSuccess()) {
                            logger.info("Thêm sản phẩm thành công: {}", name);
                            showAlert("Thành công", "Thêm sản phẩm thành công!");
                            clearFields();
                            loadMyProducts();
                        } else {
                            logger.error("Lỗi thêm: {}", response.getMessage());
                            showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Thêm sản phẩm thất bại!");
                        }
                    });
                });
            } else {
                data.put("productId", selectedProduct.getId());
                Request request = new Request(CommandType.UPDATE_PRODUCT, data);
                SocketClient.getInstance().sendRequestAsync(request, response -> {
                    Platform.runLater(() -> {
                        sourceButton.setDisable(false);
                        if (response.isSuccess()) {
                            logger.info("Cập nhật sản phẩm thành công: {}", name);
                            showAlert("Thành công", "Cập nhật sản phẩm thành công!");
                            clearFields();
                            selectedProduct = null;
                            loadMyProducts();
                        } else {
                            logger.error("Lỗi cập nhật: {}", response.getMessage());
                            showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Cập nhật thất bại!");
                        }
                    });
                });
            }
        } catch (Exception e) {
            logger.error("Error in handleAddProduct: ", e);
            showAlert("Lỗi", "Có lỗi xảy ra: " + e.getMessage());
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

        if (days > 0) {
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    @FXML
    public void toProductListController(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/ProductListController.fxml", SellingController.class);
        } catch (Exception e) {
            logger.error("Error navigating to ProductList: ", e);
        }
    }

    @FXML
    public void toAdmin(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/Admin.fxml", SellingController.class);
        } catch (Exception e) {
            logger.error("Error navigating to Admin: ", e);
        }
    }

    @FXML
    public void goToLoginScreen(ActionEvent event) {
        SocketClient.getInstance().logout(response -> {
            Platform.runLater(() -> {
                try {
                    Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    oldStage.close();
                    WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", SellingController.class);
                } catch (Exception e) {
                    logger.error("Error navigating to Login: ", e);
                }
            });
        });
    }

    @FXML
    private void toProfile(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/Profile.fxml", SellingController.class);
        } catch (Exception e) {
            logger.error("Error navigating to Profile: ", e);
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