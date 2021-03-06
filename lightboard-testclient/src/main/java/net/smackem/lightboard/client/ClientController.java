package net.smackem.lightboard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.lightboard.client.beans.DrawingBean;
import net.smackem.lightboard.client.beans.FigureBean;
import net.smackem.lightboard.client.beans.PointBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientController {
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);
    private final OSCPortOut outboundPort;
    private final Collection<PointBean> currentFigure = new ArrayList<>();
    private DrawingBean drawing;

    @FXML
    private Pane canvasContainer;
    @FXML
    private Canvas canvas;

    public ClientController() throws IOException {
        this.outboundPort = new OSCPortOut(new InetSocketAddress("localhost", 7771));
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
        if (this.drawing != null) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(2);
            for (final FigureBean figure : this.drawing.figures()) {
                renderFigure(gc, figure.points());
            }
        }
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        renderFigure(gc, this.currentFigure);
    }

    private void renderFigure(GraphicsContext gc, Collection<PointBean> points) {
        PointBean prevPt = null;
        for (final PointBean point : points) {
            if (prevPt != null) {
                gc.strokeLine(prevPt.x(), prevPt.y(), point.x(), point.y());
            }
            prevPt = point;
        }
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
        this.currentFigure.add(new PointBean(event.getX(), event.getY()));
        render();
        try {
            this.outboundPort.send(new OSCMessage("/figure/begin",
                    List.of((float) event.getX(), (float) event.getY(), 0xff, 0x80, 0x00, 0xff, 2.0f)));
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onMouseDragged(MouseEvent event) {
        this.currentFigure.add(new PointBean(event.getX(), event.getY()));
        render();
        try {
            this.outboundPort.send(new OSCMessage("/figure/point", List.of((float) event.getX(), (float) event.getY())));
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onMouseReleased(MouseEvent event) {
        this.currentFigure.clear();
        try {
            this.outboundPort.send(new OSCMessage("/figure/end", List.of((float) event.getX(), (float) event.getY())));
            downloadDrawing();
        } catch (OSCSerializeException | IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadDrawing() {
        final HttpClient http = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:7772/drawing")).build();
        final CompletableFuture<HttpResponse<String>> cf = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        cf.exceptionally(e -> {
            log.error("error downloading drawing via http", e);
            return null;
        });
        cf.thenAcceptAsync(response -> {
            log.info("http response received: {}", response);
            final ObjectMapper mapper = new ObjectMapper();
            try {
                this.drawing = mapper.readerFor(DrawingBean.class).readValue(response.body());
            } catch (IOException e) {
                log.error("error downloading drawing via http", e);
            }
            render();
        }, Platform::runLater);
    }
}
