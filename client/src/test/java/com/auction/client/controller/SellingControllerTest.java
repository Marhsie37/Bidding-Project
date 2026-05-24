package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class SellingControllerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/Selling.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Test 1: Kiểm tra form thêm sản phẩm hiển thị
    // fx:id thực tế trong Selling.fxml: txtName, txtPrice, txtDuration
    // Không có txtImageUrl — FXML dùng image picker (boxAddImage), không phải text field
    @Test
    void testAddProductFormVisible() {
        verifyThat("#txtName", isVisible());
        verifyThat("#txtPrice", isVisible());
        verifyThat("#txtDuration", isVisible());
    }

    // Test 2: Kiểm tra danh sách sản phẩm hiển thị
    @Test
    void testProductListVisible() {
        verifyThat("#vboxDisplay", isVisible());
    }
}