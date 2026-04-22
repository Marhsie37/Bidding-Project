package Part1;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class UserItemController {
    @FXML private Label lblInfo;
    @FXML private Button btnBan, btnDelete;


    public void setData(User user) {
        lblInfo.setText(user.getFullname() + " | " + user.getEmail() + " | " + user.getStatus());

        if (user.getStatus().equals("ACTIVE")) {
            btnBan.setText("Ban");
            btnBan.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        } else {
            btnBan.setText("Unban");
            btnBan.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        }
    }


    public Button getBtnBan() { return btnBan; }
    public Button getBtnDelete() { return btnDelete; }
}