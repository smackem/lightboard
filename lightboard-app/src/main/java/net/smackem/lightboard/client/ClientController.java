package net.smackem.lightboard.client;

import com.illposed.osc.transport.udp.OSCPortOut;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientController {
    private final OSCPortOut outboundPort;

    @FXML
    private Canvas canvas;

    public ClientController() throws IOException {
        this.outboundPort = new OSCPortOut(new InetSocketAddress("localhost", 7770));
        this.outboundPort.connect();
    }
    
    @FXML
    private void initialize() {
        this.canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        this.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        this.canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        Platform.runLater(() ->
                this.canvas.getScene().getWindow().setOnCloseRequest(this::onWindowCloseRequest));
        render();
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.setFill(Color.BLUEVIOLET);
        gc.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    private void onWindowCloseRequest(WindowEvent windowEvent) {
        try {
            this.outboundPort.disconnect();
            this.outboundPort.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onMousePressed(MouseEvent event) {
    }

    private void onMouseDragged(MouseEvent event) {
    }

    private void onMouseReleased(MouseEvent event) {
    }
}
