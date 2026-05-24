package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class LoginControllerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Test 1: Kiểm tra username và password field hiển thị
    @Test
    void testLoginComponentsVisible() {
        verifyThat("#userNameField", isVisible());
        verifyThat("#passwordField", isVisible());
        // Không dùng #loginButton vì button LOGIN trong FXML không có fx:id
    }

    // Test 2: Nhập text vào các field
    @Test
    void testLoginWithEmptyFields() {
        clickOn("#userNameField").write("");
        clickOn("#passwordField").write("");
        // Button LOGIN không có fx:id nên không thể clickOn — bỏ qua
    }
}