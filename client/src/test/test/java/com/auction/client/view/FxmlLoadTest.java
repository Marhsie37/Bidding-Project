package com.auction.client.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FxmlLoadTest {

  @Test
  void testLoginFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/LoginController.fxml"));
    assertNotNull(root);
  }

  @Test
  void testAdminFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Admin.fxml"));
    assertNotNull(root);
  }

  @Test
  void testSellingFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Selling.fxml"));
    assertNotNull(root);
  }

  @Test
  void testProductListFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/ProductListController.fxml"));
    assertNotNull(root);
  }

  @Test
  void testProfileFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Profile.fxml"));
    assertNotNull(root);
  }

  @Test
  void testRegisterFxmlLoads() throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/RegisterController.fxml"));
    assertNotNull(root);
  }
}