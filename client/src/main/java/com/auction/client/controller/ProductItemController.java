package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductItemController {

    @FXML private Label lblName, lblPrice, lblTimer;
    @FXML private ImageView imgView;
    @FXML private Button btnEdit, btnDelete;

    public void setData(Product p) {
        if (p == null) return;

        lblName.setText(p.getName());
        lblPrice.setText(p.getCurrentPrice() + " VNĐ");

        String url = p.getImageUrl();
        if (url != null && !url.isEmpty()) {
            try {
                if (!url.startsWith("http") && !url.startsWith("file:")) {
                    url = "file:" + url;
                }
                Image img = new Image(url, true);
                imgView.setImage(img);
            } catch (Exception e) {
                System.out.println("Lỗi tải ảnh: " + e.getMessage());
            }
        }
    }

    public Button getBtnEdit() { return btnEdit; }
    public Button getBtnDelete() { return btnDelete; }
    public Label getLblTimer() { return lblTimer; }
}