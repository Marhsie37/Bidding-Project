package Part1;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;

public class WindowManager {

    public static void openUndecoratedWindow(String fxmlPath, Object caller) {
        try {
            FXMLLoader loader = new FXMLLoader(caller.getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();


            stage.initStyle(StageStyle.UNDECORATED);

            Scene scene = new Scene(root);
            stage.setScene(scene);


            stage.sizeToScene();


            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}