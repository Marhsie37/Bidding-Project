package com.auction.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductListController implements Initializable {

    @FXML private VBox vboxGallery;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshGallery();
    }


    public void refreshGallery() {
        vboxGallery.getChildren().clear();
        vboxGallery.setSpacing(10);
        for (Product p : DataManager.sharedProductList) {
            vboxGallery.getChildren().add(createProductCard(p));
        }
    }

    private HBox createProductCard(Product p) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 10; -fx-background-color: #ffffff; -fx-cursor: hand;");


        ImageView img = new ImageView();
        try {
            img.setImage(new Image(p.getImageUrl(), true));
            img.setFitWidth(100);
            img.setFitHeight(100);
            img.setPreserveRatio(true);
        } catch (Exception e) { }


        VBox details = new VBox(10);
        Label name = new Label("Tên: " + p.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label price = new Label("Giá hiện tại: " + p.getPrice() + " $");
        price.setStyle("-fx-text-fill: #2c3e50;");

        Label lblTime = new Label();
        lblTime.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        details.getChildren().addAll(name, price, lblTime);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int r = p.getRemainingSeconds();
            lblTime.setText(r <= 0 ? "HẾT HẠN" : "Còn lại: " + r + "s");
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();


        card.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Part1/ProductDetailController.fxml"));
                Parent root = loader.load();

                ProductDetailController controller = loader.getController();
                controller.setProductData(p);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Chi tiết đấu giá");


                stage.setOnHidden(e -> refreshGallery());

                stage.show();
            } catch (IOException e) {
                System.err.println("Lỗi load FXML! Kiểm tra tên file trong resources/Part1/");
                e.printStackTrace();
            }
        });

        card.getChildren().addAll(img, details);
        return card;
    }

    @FXML
    public void toSelling(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/Selling.fxml"));
        Stage window = (Stage) vboxGallery.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }

    @FXML
    public void toAdmin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/Admin.fxml"));
        Stage window = (Stage) vboxGallery.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }
}