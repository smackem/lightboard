package net.smackem.lightboard.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.WindowEvent;
import net.smackem.lightboard.io.MessageExchangeHost;
import net.smackem.lightboard.messaging.*;
import net.smackem.lightboard.model.Document;
import net.smackem.lightboard.model.Drawing;
import net.smackem.lightboard.model.Figure;
import net.smackem.lightboard.model.Rgba;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.Doc;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private final Document document = new Document();
    private final MessageExchangeHost mex;

    @FXML
    private Canvas canvas;
    @FXML
    private Pane canvasContainer;

    public MainController() throws IOException {
        this.mex = new MessageExchangeHost(() -> this.document);
        this.mex.inboundMessagePublisher().subscribe(new InboundMessageSubscriber());
    }

    @FXML
    private void initialize() {
        Platform.runLater(() ->
            this.canvas.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowClosed));
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
        for (final Figure figure : this.document.drawing().figures()) {
            Coordinate prevPt = null;
            final Rgba rgba = figure.color();
            gc.setStroke(Color.rgb(rgba.r(), rgba.g(), rgba.b(), rgba.a() / 255.0));
            gc.setLineWidth(figure.strokeWidth());
            for (final Coordinate point : figure.points()) {
                if (prevPt != null) {
                    gc.strokeLine(prevPt.getX(), prevPt.getY(), point.getX(), point.getY());
                }
                prevPt = point;
            }
        }
    }

    private void onWindowClosed(WindowEvent windowEvent) {
        try {
            this.mex.close();
        } catch (Exception e) {
            log.error("error closing mex", e);
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof InitSizeMessage initSize) {
            if (this.document.drawing().isBlank() == false) {
                this.document.insertNewDrawing();
            }
            this.canvas.setWidth(initSize.width());
            this.canvas.setHeight(initSize.height());
            render();
            return;
        }
        if (message instanceof FigureBeginMessage figureBegin) {
            this.document.drawing().beginFigure(figureBegin.point(), figureBegin.color(), figureBegin.strokeWidth());
            return;
        }
        if (message instanceof FigurePointMessage figurePoint) {
            this.document.drawing().addPoint(figurePoint.point());
            render();
            return;
        }
        if (message instanceof FigureEndMessage figureEnd) {
            this.document.drawing().endFigure(figureEnd.point());
            render();
            return;
        }
        if (message instanceof FigureRemoveMessage figureRemove) {
            this.document.drawing().removeFigure(figureRemove.figureIndex());
            render();
            return;
        }
        if (message instanceof RedrawMessage) {
            render();
            return;
        }
        throw new IllegalArgumentException("unsupported message type " + message.getClass());
    }

    private class InboundMessageSubscriber implements Flow.Subscriber<Message> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Message item) {
            Platform.runLater(() -> {
                handleMessage(item);
                subscription.request(1);
            });
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("error consuming message", throwable);
        }

        @Override
        public void onComplete() {
            log.info("message stream complete");
        }
    }
}
