package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.shared.model.Product;
import com.auction.shared.protocol.CommandType;
import com.auction.shared.protocol.Request;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProductListController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ProductListController.class);

    @FXML private VBox vboxGallery;
    @FXML private HBox imageHBox;
    @FXML private Pane sliderContainer;
    @FXML private TextField searchField;

    private int currentSlideIndex = 0;
    private final int totalSlides = 3;
    private final double slideWidth = 612.0;
    private Timeline autoSlideTimer;
    private List<Product> allProducts = new java.util.ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("ProductListController initialized");
        setupImageSlider();
        refreshGallery();

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                searchProducts(newValue);
            });
        }
    }

    private void searchProducts(String keyword) {
        if (vboxGallery == null) return;

        if (keyword == null || keyword.trim().isEmpty()) {
            displayProducts(allProducts);
            return;
        }

        String searchKeyword = keyword.trim().toLowerCase();
        List<Product> filtered = allProducts.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(searchKeyword))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            vboxGallery.getChildren().clear();
            Label emptyLabel = new Label("Không tìm thấy sản phẩm nào phù hợp với \"" + keyword + "\"");
            emptyLabel.setStyle("-fx-padding: 20; -fx-text-fill: gray; -fx-font-size: 14;");
            vboxGallery.getChildren().add(emptyLabel);
        } else {
            displayProducts(filtered);
        }
    }

    private void setupImageSlider() {
        if (imageHBox == null || sliderContainer == null) {
            logger.warn("Image slider components not found");
            return;
        }

        imageHBox.setTranslateX(0);

        Rectangle clip = new Rectangle(slideWidth, 234);
        sliderContainer.setClip(clip);

        autoSlideTimer = new Timeline(new KeyFrame(Duration.seconds(4), e -> goNext()));
        autoSlideTimer.setCycleCount(Timeline.INDEFINITE);
        autoSlideTimer.play();

        sliderContainer.setOnMouseEntered(event -> autoSlideTimer.pause());
        sliderContainer.setOnMouseExited(event -> autoSlideTimer.play());
    }

    @FXML
    public void goNext() {
        currentSlideIndex = (currentSlideIndex + 1) % totalSlides;
        updateSliderPosition();
    }

    @FXML
    public void goPrevious() {
        currentSlideIndex = (currentSlideIndex - 1 + totalSlides) % totalSlides;
        updateSliderPosition();
    }

    private void updateSliderPosition() {
        double targetX = -(currentSlideIndex * slideWidth);
        TranslateTransition transition = new TranslateTransition(Duration.millis(700), imageHBox);
        transition.setToX(targetX);
        transition.play();
    }

    public void refreshGallery() {
        if (vboxGallery == null) return;

        Request request = new Request(CommandType.GET_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (!response.isSuccess() || response.getData() == null) {
                    logger.error("Lỗi lấy danh sách sản phẩm: {}", response.getMessage());
                    vboxGallery.getChildren().clear();
                    Label errorLabel = new Label("Không thể tải danh sách sản phẩm");
                    errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20;");
                    vboxGallery.getChildren().add(errorLabel);
                    return;
                }

                Map<String, Object> data = response.getData();
                Object productsObj = data.get("products");

                List<Product> products = null;
                if (productsObj instanceof List) {
                    products = (List<Product>) productsObj;
                }

                if (products == null || products.isEmpty()) {
                    logger.info("Không có sản phẩm nào");
                    vboxGallery.getChildren().clear();
                    Label emptyLabel = new Label("Hiện chưa có sản phẩm đấu giá nào");
                    emptyLabel.setStyle("-fx-padding: 20; -fx-text-fill: gray;");
                    vboxGallery.getChildren().add(emptyLabel);
                    allProducts.clear();
                    return;
                }

                allProducts = products;
                displayProducts(allProducts);
            });
        });
    }

    private void displayProducts(List<Product> products) {
        if (vboxGallery == null) return;

        vboxGallery.getChildren().clear();
        vboxGallery.setSpacing(15);

        for (Product p : products) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductItemStage.fxml"));
                Parent productCard = loader.load();

                ProductItemController itemController = loader.getController();
                itemController.setData(p);

                productCard.setOnMouseClicked(event -> openProductDetail(p));

                vboxGallery.getChildren().add(productCard);
                logger.debug("Đã thêm sản phẩm: {}", p.getName());

            } catch (IOException e) {
                logger.error("Lỗi khi nạp FXML cho sản phẩm: {}", p.getName(), e);
            }
        }

        logger.info("Đã hiển thị {} sản phẩm", products.size());
    }

    private void openProductDetail(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductDetailController.fxml"));
            Parent root = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setProductData(p);

            Stage stage = (Stage) vboxGallery.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            logger.error("Lỗi khi mở chi tiết sản phẩm: {}", p.getName(), e);
        }
    }

    @FXML
    private void toSelling(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/Selling.fxml", ProductListController.class);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển sang màn hình bán hàng", e);
        }
    }

    @FXML
    private void toAdmin(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/Admin.fxml", ProductListController.class);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển sang màn hình admin", e);
        }
    }

    @FXML
    private void goToLoginScreen(ActionEvent event) {
        SocketClient.getInstance().logout(response -> {
            Platform.runLater(() -> {
                try {
                    Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    oldStage.close();
                    WindowManager.openWindow("/com/auction/client/view/LoginController.fxml", ProductListController.class);
                } catch (Exception e) {
                    logger.error("Lỗi khi chuyển sang màn hình đăng nhập", e);
                }
            });
        });
    }

    @FXML
    private void toProfile(ActionEvent event) {
        try {
            Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            oldStage.close();
            WindowManager.openWindow("/com/auction/client/view/Profile.fxml", ProductListController.class);
        } catch (Exception e) {
            logger.error("Lỗi khi chuyển sang màn hình profile", e);
        }
    }
}