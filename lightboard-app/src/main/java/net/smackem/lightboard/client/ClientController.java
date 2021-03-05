package net.smackem.lightboard.client;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class ClientController {
    private final OSCPortOut outboundPort;

    @FXML
    private Pane canvasContainer;
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
        this.canvas.widthProperty().bind(this.canvasContainer.widthProperty());
        this.canvas.heightProperty().bind(this.canvasContainer.heightProperty());
        this.canvas.widthProperty().addListener(this::onCanvasResize);
        this.canvas.heightProperty().addListener(this::onCanvasResize);
        render();
    }

    private void onCanvasResize(Observable observable) {
        try {
            this.outboundPort.send(new OSCMessage("/init/size", List.of((int) this.canvas.getWidth(), (int) this.canvas.getHeight())));
        } catch (IOException | OSCSerializeException e) {
            e.printStackTrace();
        }
        render();
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
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
        try {
            this.outboundPort.send(new OSCMessage("/figure/begin", List.of((float) event.getX(), (float) event.getY())));
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onMouseDragged(MouseEvent event) {
        try {
            this.outboundPort.send(new OSCMessage("/figure/point", List.of((float) event.getX(), (float) event.getY())));
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onMouseReleased(MouseEvent event) {
        try {
            this.outboundPort.send(new OSCMessage("/figure/end", List.of((float) event.getX(), (float) event.getY())));
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }
}
