package net.smackem.lightboard.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {
    private static String fxml;
    private Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        this.scene = new Scene(loadFxml(fxml), 640, 480);
        stage.setScene(this.scene);
        stage.show();
    }

    private static Parent loadFxml(String fxml) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        if (args.length > 0 && Objects.equals(args[0], "client")) {
            fxml = "/net/smackem/lightboard/client/client";
        } else {
            fxml = "main";
        }
        launch();
    }
}