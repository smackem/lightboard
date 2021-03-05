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

    @Override
    public void start(Stage stage) throws IOException {
        final Scene scene = new Scene(loadFxml(fxml), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    private static Parent loadFxml(String fxml) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        if (args.length > 0 && "client".equals(args[0])) {
            fxml = "/net/smackem/lightboard/client/client";
        } else {
            fxml = "main";
        }
        launch();
    }
}