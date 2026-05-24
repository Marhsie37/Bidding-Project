package com.auction.client.view;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FxmlLoadTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        // Khởi động JavaFX Platform nếu chưa chạy
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // Platform đã được khởi động rồi, bỏ qua
        }
    }

    private Parent loadFxml(String path) throws Exception {
        AtomicReference<Parent> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource(path));
                result.set(root);
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        if (error.get() != null) throw error.get();
        return result.get();
    }

    @Test
    void testLoginFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/LoginController.fxml");
        assertNotNull(root);
    }

    @Test
    void testAdminFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/Admin.fxml");
        assertNotNull(root);
    }

    @Test
    void testSellingFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/Selling.fxml");
        assertNotNull(root);
    }

    @Test
    void testProductListFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/ProductListController.fxml");
        assertNotNull(root);
    }

    @Test
    void testProfileFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/Profile.fxml");
        assertNotNull(root);
    }

    @Test
    void testRegisterFxmlLoads() throws Exception {
        Parent root = loadFxml("/com/auction/client/view/RegisterController.fxml");
        assertNotNull(root);
    }
}