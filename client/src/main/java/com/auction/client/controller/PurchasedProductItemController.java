package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PurchasedProductItemController {

    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private ImageView imgProduct;

    public void setData(Product p) {
        if (p == null) return;

        lblName.setText(p.getName());
        lblPrice.setText(String.format("%,.0f", p.getCurrentPrice()) + " VNĐ");

        String url = p.getImageUrl();
        System.out.println("🔍 URL ảnh: " + url);  // THÊM DÒNG NÀY

        if (url != null && !url.isEmpty()) {
            try {
                // Thử load ảnh
                Image img = new Image(url, true);
                imgProduct.setImage(img);
                System.out.println("✅ Đã load ảnh thành công");
            } catch (Exception e) {
                System.out.println("❌ Lỗi load ảnh: " + e.getMessage());
            }
        }
    }
}