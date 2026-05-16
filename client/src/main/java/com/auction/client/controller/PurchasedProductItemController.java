package com.auction.client.controller;

import com.auction.shared.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class PurchasedProductItemController {

    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private ImageView imgProduct;
    private static final Logger logger = LoggerFactory.getLogger(PurchasedProductItemController.class);
    public void setData(Product p) {
        if (p == null) return;

        lblName.setText(p.getName());
        lblPrice.setText(String.format("%,.0f", p.getCurrentPrice()) + " VNĐ");

        String url = p.getImageUrl();
        logger.info("🔍 URL ảnh: {}" , url);  // THÊM DÒNG NÀY

        if (url != null && !url.isEmpty()) {
            try {
                // Thử load ảnh
                Image img = new Image(url, true);
                imgProduct.setImage(img);
                logger.info("✅ Đã load ảnh thành công");
            } catch (Exception e) {
                logger.error("❌ Lỗi load ảnh: " , e);
            }
        }
    }
}