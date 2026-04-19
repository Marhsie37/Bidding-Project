package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Admin {
    @FXML private VBox vBoxDisplay;
    @FXML private VBox vBoxProducts;

    @FXML
    public void initialize() {
        renderUserList();
        renderProductList();
    }


    public void renderUserList() {
        if (vBoxDisplay == null) return;

        vBoxDisplay.getChildren().clear();
        vBoxDisplay.setSpacing(10);

        for (User user : DataManager.allUsers) {
            if (user.getRole().equals("ADMIN")) continue;

            HBox userRow = new HBox(20);
            userRow.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: white;");

            Label infoLabel = new Label("Tên: " + user.getFullname() + " | Vai trò: " + user.getRole() + " | Trạng thái: " + user.getStatus());
            infoLabel.setMinWidth(300);

            Button banBtn = new Button(user.getStatus().equals("ACTIVE") ? "Ban" : "Unban");
            banBtn.setStyle(user.getStatus().equals("ACTIVE") ? "-fx-background-color: red; -fx-text-fill: white;" : "-fx-background-color: green; -fx-text-fill: white;");

            banBtn.setOnAction(e -> {
                if (user.getStatus().equals("ACTIVE")) {
                    user.setStatus("BANNED");
                } else {
                    user.setStatus("ACTIVE");
                }
                renderUserList();
            });

            userRow.getChildren().addAll(infoLabel, banBtn);
            vBoxDisplay.getChildren().add(userRow);
        }
    }

    public void renderProductList() {
        if (vBoxProducts == null) return;

        vBoxProducts.getChildren().clear();
        vBoxProducts.setSpacing(10);

        for (Product p : DataManager.sharedProductList) {
            HBox productRow = new HBox(20);
            productRow.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-background-color: #f9f9f9;");
            productRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            ImageView imgView = new ImageView();
            try {
                imgView.setImage(new Image(p.getImageUrl(), true));
                imgView.setFitWidth(60);
                imgView.setFitHeight(60);
                imgView.setPreserveRatio(true);
            } catch (Exception e) {
                System.out.println("Lỗi link ảnh: " + p.getName());
            }

            Label infoLabel = new Label("Sản phẩm: " + p.getName() + " | Giá: " + p.getPrice() + " $");
            infoLabel.setMinWidth(250);

            Button deleteBtn = new Button("Xóa (Admin)");
            deleteBtn.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-cursor: hand;");

            deleteBtn.setOnAction(e -> {
                DataManager.sharedProductList.remove(p);
                renderProductList();
            });

            productRow.getChildren().addAll(imgView, infoLabel, deleteBtn);
            vBoxProducts.getChildren().add(productRow);
        }
    }

    @FXML
    public void toLogin(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("LoginController.fxml"));
        Stage window =(Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.sizeToScene();
        window.centerOnScreen();
        window.show();
    }

    @FXML
    public void toSelling(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Selling.fxml"));
        Stage window =(Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.sizeToScene();
        window.centerOnScreen();
        window.show();
    }


}