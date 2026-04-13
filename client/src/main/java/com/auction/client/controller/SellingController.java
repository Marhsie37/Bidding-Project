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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SellingController implements Initializable {
    @FXML private VBox vboxDisplay;
    @FXML private TextField txtName, txtPrice, txtImageUrl, txtDuration;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        vboxDisplay.getChildren().clear();
        vboxDisplay.setSpacing(10);
        for (Product p : DataManager.sharedProductList) {
            reconstructProductUI(p);
        }
    }


    private void reconstructProductUI(Product p) {

        HBox productRow = new HBox(15);
        productRow.setAlignment(Pos.CENTER_LEFT);
        productRow.setStyle("-fx-padding: 10; -fx-background-color: #ffffff; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");


        ImageView imgView = new ImageView();
        try {
            imgView.setImage(new Image(p.getImageUrl(), true));
            imgView.setFitWidth(80);
            imgView.setFitHeight(80);
            imgView.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Lỗi link ảnh: " + p.getName());
        }


        VBox details = new VBox(5);
        Label lblName = new Label(p.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblPrice = new Label(p.getPrice() + " $");
        lblPrice.setStyle("-fx-text-fill: #e44d26; -fx-font-weight: bold;");


        Label lblCountdown = new Label();
        lblCountdown.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        details.getChildren().addAll(lblName, lblPrice, lblCountdown);


        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int remaining = p.getRemainingSeconds(); // Lấy số giây còn lại thực tế

            if (remaining <= 0) {
                lblCountdown.setText("HẾT HẠN!");
                lblCountdown.setStyle("-fx-text-fill: #7f8c8d;");
                productRow.setOpacity(0.6);
            } else {
                lblCountdown.setText("Thời gian: " + remaining + "s");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();


        Button btnDelete = new Button("Xóa");
        btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            timeline.stop();
            vboxDisplay.getChildren().remove(productRow);
            DataManager.sharedProductList.remove(p);
        });


        productRow.getChildren().addAll(imgView, details, btnDelete);
        vboxDisplay.getChildren().add(productRow);
    }

    @FXML
    void handleAddProduct() {
        String name = txtName.getText().trim();
        String price = txtPrice.getText().trim();
        String url = txtImageUrl.getText().trim();
        String durationStr = txtDuration.getText().trim();

        if (name.isEmpty() || price.isEmpty() || url.isEmpty() || durationStr.isEmpty()) return;

        try {
            int totalSeconds = Integer.parseInt(durationStr);

            Product newP = new Product(name, price, url, totalSeconds);


            DataManager.sharedProductList.add(newP);
            reconstructProductUI(newP);

            clearFields();
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Thời gian phải là số!");
        }
    }

    @FXML
    public void toProductListController(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Part1/ProductListController.fxml"));

        Stage window = (Stage) vboxDisplay.getScene().getWindow();
        window.setScene(new Scene(root));
        window.show();
    }

    private void clearFields() {
        txtName.clear(); txtPrice.clear(); txtImageUrl.clear(); txtDuration.clear();
        txtName.requestFocus();
    }
}