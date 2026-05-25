package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

public class AdminControllerTest extends ApplicationTest {

  private Admin controller;

  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/Admin.fxml"));
    Parent root = loader.load();
    controller = loader.getController();
    stage.setScene(new Scene(root));
    stage.show();
  }

  // Test 1: Kiểm tra tab User có hiển thị không
  @Test
  void testUserTabVisible() {
    verifyThat("#vBoxDisplay", isVisible());
  }

  // Test 2: Kiểm tra tab Product có hiển thị không
  @Test
  void testProductTabVisible() {
    verifyThat("#vBoxProducts", isVisible());
  }
}