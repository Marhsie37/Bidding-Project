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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ProductListController implements Initializable {

    @FXML private VBox vboxGallery;
    @FXML private HBox imageHBox;
    @FXML private Pane sliderContainer;

    @FXML private TextField searchField;

    private int currentSlideIndex = 0;
    private final int totalSlides = 3;
    private final double slideWidth = 612.0;
    private Timeline autoSlideTimer;
    private static final Logger logger = LoggerFactory.getLogger(ProductListController.class);
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupImageSlider();
        refreshGallery();  // Gọi lấy danh sách sản phẩm từ server

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchProducts(newValue);
        });
    }

    private void searchProducts(String keyword) {
        if (vboxGallery == null) return;
        vboxGallery.getChildren().clear();
        vboxGallery.setSpacing(15);

        Request request = new Request(CommandType.GET_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    List<Product> products = (List<Product>) data.get("products");

                    if (products != null) {
                        // Lọc sản phẩm theo từ khóa
                        List<Product> filtered = products.stream()
                                .filter(p -> keyword == null || keyword.isEmpty() ||
                                        p.getName().toLowerCase().contains(keyword.toLowerCase()))
                                .collect(Collectors.toList());

                        for (Product p : filtered) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductItemStage.fxml"));
                                Parent productCard = loader.load();

                                ProductItemController itemController = loader.getController();
                                itemController.setData(p);

                                productCard.setOnMouseClicked(event -> {
                                    openProductDetail(p);
                                });

                                vboxGallery.getChildren().add(productCard);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        });
    }

    private void setupImageSlider() {
        if (imageHBox == null || sliderContainer == null) return;

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

    // ✅ SỬA METHOD NÀY - GỌI SERVER LẤY DANH SÁCH SẢN PHẨM
    public void refreshGallery() {
        if (vboxGallery == null) return;
        vboxGallery.getChildren().clear();
        vboxGallery.setSpacing(15);

        // Gọi server lấy danh sách sản phẩm
        Request request = new Request(CommandType.GET_PRODUCTS, new HashMap<>());
        SocketClient.getInstance().sendRequestAsync(request, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    List<Product> products = (List<Product>) data.get("products");

                    if (products != null) {
                        for (Product p : products) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductItemStage.fxml"));
                                Parent productCard = loader.load();

                                ProductItemController itemController = loader.getController();
                                itemController.setData(p);

                                productCard.setOnMouseClicked(event -> {
                                    openProductDetail(p);
                                });

                                vboxGallery.getChildren().add(productCard);

                            } catch (IOException e) {
                                logger.error("Lỗi khi nạp mẫu sản phẩm FXML");
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    logger.error("Lỗi lấy danh sách sản phẩm: " + response.getMessage());
                }
            });
        });
    }



    private void openProductDetail(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ProductDetailController.fxml"));
            Parent root = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setProductData(p);

            Stage stage = (Stage) vboxGallery.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toSelling(ActionEvent event) throws IOException {
        changeScene(event, "/com/auction/client/view/Selling.fxml");
    }

    @FXML
    private void toAdmin(ActionEvent event) throws IOException {
        changeScene(event, "/com/auction/client/view/Admin.fxml");
    }

    @FXML
    private void goToLoginScreen(ActionEvent event) throws IOException {
        changeScene(event, "/com/auction/client/view/LoginController.fxml");
    }

    private void changeScene(ActionEvent event, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }

    @FXML
    private void toProfile(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Profile.fxml"));
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.centerOnScreen();
        window.show();
    }
}