package net.smackem.lightboard.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.lightboard.io.MessageExchangeHost;
import net.smackem.lightboard.messaging.*;
import net.smackem.lightboard.model.Drawing;
import net.smackem.lightboard.model.Figure;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Flow;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private final Drawing drawing = new Drawing();
    private final MessageExchangeHost mex;

    @FXML
    private Canvas canvas;

    public MainController() throws IOException {
        this.mex = new MessageExchangeHost(() -> this.drawing);
        this.mex.inboundMessagePublisher().subscribe(new InboundMessageSubscriber());
    }

    @FXML
    private void initialize() {
        Platform.runLater(() ->
            this.canvas.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowClosed));
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        for (final Figure figure : this.drawing.figures()) {
            Coordinate prevPt = null;
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
            this.drawing.clear();
            this.canvas.setWidth(initSize.width());
            this.canvas.setHeight(initSize.height());
            render();
            return;
        }
        if (message instanceof FigureBeginMessage figureBegin) {
            this.drawing.beginFigure(figureBegin.point());
            return;
        }
        if (message instanceof FigurePointMessage figurePoint) {
            this.drawing.addPoint(figurePoint.point());
            render();
            return;
        }
        if (message instanceof FigureEndMessage figureEnd) {
            this.drawing.endFigure(figureEnd.point());
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
