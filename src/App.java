import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("Vector Quantization");
        primaryStage.setScene(scene);
        primaryStage.setMaxWidth(1270);
        primaryStage.setMaxHeight(720);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);

        primaryStage.show();
    }
}