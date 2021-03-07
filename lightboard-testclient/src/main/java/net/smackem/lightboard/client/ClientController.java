package net.smackem.lightboard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.lightboard.client.beans.DrawingBean;
import net.smackem.lightboard.client.beans.FigureBean;
import net.smackem.lightboard.client.beans.PointBean;
import net.smackem.lightboard.client.beans.RgbaBean;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
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
import java.util.stream.Collectors;

public class ClientController {
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);
    private final OSCPortOut outboundPort;
    private final FigureBean currentFigure = new FigureBean(new RgbaBean(255, 255, 255, 255), 2.0);
    private DrawingBean drawing;
    private DrawTool drawTool = new DefaultDrawTool();

    @FXML
    private Pane canvasContainer;
    @FXML
    private Canvas canvas;
    @FXML
    private ToggleButton toggleEraseButton  ;

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
                renderFigure(gc, figure);
            }
        }
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        renderFigure(gc, this.currentFigure);
    }

    private void renderFigure(GraphicsContext gc, FigureBean figure) {
        final RgbaBean rgba = figure.color();
        gc.setStroke(Color.rgb(rgba.r(), rgba.g(), rgba.b(), rgba.a() / 255.0));
        gc.setLineWidth(figure.strokeWidth());
        PointBean prevPt = null;
        for (final PointBean point : figure.points()) {
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
        this.drawTool.onMousePressed(event.getX(), event.getY());
    }

    private void onMouseDragged(MouseEvent event) {
        this.drawTool.onMouseDrag(event.getX(), event.getY());
    }

    private void onMouseReleased(MouseEvent event) {
        this.drawTool.onMouseReleased(event.getX(), event.getY());
    }

    private void requestDrawing(String path, String method) {
        final HttpClient http = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:7772" + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
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

    @FXML
    private void newDrawing(ActionEvent actionEvent) {
        requestDrawing("/drawing/new", "POST");
    }

    @FXML
    private void prevDrawing(ActionEvent actionEvent) {
        requestDrawing("/drawing/prev", "POST");
    }

    @FXML
    private void nextDrawing(ActionEvent actionEvent) {
        requestDrawing("/drawing/next", "POST");
    }

    @FXML
    private void toggleErase(ActionEvent actionEvent) {
        this.drawTool = this.toggleEraseButton.isSelected()
                ? new EraseDrawTool()
                : new DefaultDrawTool();
    }

    private class DefaultDrawTool implements DrawTool {
        @Override
        public void onMousePressed(double x, double y) {
            currentFigure.points().add(new PointBean(x, y));
            render();
            try {
                outboundPort.send(new OSCMessage("/figure/begin",
                        List.of((float) x, (float) y, 0xff, 0x80, 0x00, 0xff, 2.0f)));
            } catch (OSCSerializeException | IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMouseDrag(double x, double y) {
            currentFigure.points().add(new PointBean(x, y));
            render();
            try {
                outboundPort.send(new OSCMessage("/figure/point", List.of((float) x, (float) y)));
            } catch (OSCSerializeException | IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMouseReleased(double x, double y) {
            currentFigure.points().clear();
            try {
                outboundPort.send(new OSCMessage("/figure/end", List.of((float) x, (float) y)));
            } catch (OSCSerializeException | IOException e) {
                e.printStackTrace();
            }
            requestDrawing("/drawing", "GET");
        }
    }

    private class EraseDrawTool implements DrawTool {
        private final GeometryFactory geometryFactory;
        private final List<Geometry> figureGeometries;

        EraseDrawTool() {
            this.geometryFactory = new GeometryFactory();
            this.figureGeometries = drawing.figures().stream()
                    .map(figure -> figure.points().stream()
                            .map(pt -> new Coordinate(pt.x(), pt.y()))
                            .toArray(Coordinate[]::new))
                    .map(this.geometryFactory::createLineString)
                    .collect(Collectors.toList());
        }

        @Override
        public void onMousePressed(double x, double y) {
        }

        @Override
        public void onMouseDrag(double x, double y) {
            final Geometry cursor = this.geometryFactory.createPoint(new Coordinate(x, y));
            final List<Integer> figureIndicesToRemove = new ArrayList<>();
            for (int i = 0; i < this.figureGeometries.size(); i++) {
                final Geometry geometry = this.figureGeometries.get(i);
                if (geometry.distance(cursor) < 5.0) {
                    figureIndicesToRemove.add(i);
                }
            }
            final List<Geometry> geometriesToRemove = new ArrayList<>();
            final List<FigureBean> figuresToRemove = new ArrayList<>();
            for (final int figureIndex : figureIndicesToRemove) {
                geometriesToRemove.add(this.figureGeometries.get(figureIndex));
                figuresToRemove.add(drawing.figures().get(figureIndex));
                try {
                    outboundPort.send(new OSCMessage("/figure/remove", List.of((float) x, (float) y, figureIndex)));
                } catch (OSCSerializeException | IOException e) {
                    e.printStackTrace();
                }
            }
            drawing.figures().removeAll(figuresToRemove);
            this.figureGeometries.removeAll(geometriesToRemove);
            if (figuresToRemove.isEmpty() == false) {
                render();
            }
        }

        @Override
        public void onMouseReleased(double x, double y) {
            requestDrawing("/drawing", "GET");
        }
    }
}
