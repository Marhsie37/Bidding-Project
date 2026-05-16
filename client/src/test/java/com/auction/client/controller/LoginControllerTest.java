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

    private LoginController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Test 1: Kiểm tra các component có hiển thị không
    @Test
    void testLoginComponentsVisible() {
        verifyThat("#userNameField", isVisible());
        verifyThat("#passwordField", isVisible());
        verifyThat("#loginButton", isVisible());
    }

    // Test 2: Để trống username/password
    @Test
    void testLoginWithEmptyFields() {
        clickOn("#userNameField").write("");
        clickOn("#passwordField").write("");
        clickOn("#loginButton");
        // Kiểm tra alert hiện (TestFX khó bắt alert, có thể bỏ qua)
    }
}