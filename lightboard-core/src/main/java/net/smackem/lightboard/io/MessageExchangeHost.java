package net.smackem.lightboard.io;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Joiner;
import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.smackem.lightboard.messaging.*;
import net.smackem.lightboard.model.Document;
import net.smackem.lightboard.model.Drawing;
import net.smackem.lightboard.model.Rgba;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class MessageExchangeHost implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MessageExchangeHost.class);
    public static final int DEFAULT_OSC_PORT = 7771;
    public static final int DEFAULT_HTTP_PORT = 7772;

    private final OSCPortIn inbound;
    private final SubmissionPublisher<Message> inboundMessagePublisher = new SubmissionPublisher<>();
    private final Supplier<Document> documentSupplier;
    private final HttpServer httpServer;
    private final ExecutorService httpExecutor;

    public MessageExchangeHost(Supplier<Document> documentSupplier) throws IOException {
        this.documentSupplier = Objects.requireNonNull(documentSupplier);
        this.inbound = new OSCPortIn(DEFAULT_OSC_PORT);
        this.inbound.addPacketListener(new PacketListener());
        this.inbound.startListening();
        this.httpExecutor = Executors.newSingleThreadExecutor();
        this.httpServer = HttpServer.create(new InetSocketAddress(DEFAULT_HTTP_PORT), 16);
        this.httpServer.setExecutor(this.httpExecutor);
        this.httpServer.createContext("/drawing", this::handleDrawingRequest);
        this.httpServer.createContext("/drawing/new", this::handleNewDrawingRequest);
        this.httpServer.createContext("/drawing/prev", this::handlePrevDrawingRequest);
        this.httpServer.createContext("/drawing/next", this::handleNextDrawingRequest);
        this.httpServer.start();
    }

    public Flow.Publisher<Message> inboundMessagePublisher() {
        return this.inboundMessagePublisher;
    }

    @Override
    public void close() throws Exception {
        this.inboundMessagePublisher.close();
        this.inbound.close();
        this.httpExecutor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        this.httpExecutor.awaitTermination(10, TimeUnit.SECONDS);
        this.httpServer.stop(0);
    }

    private void handleDrawingRequest(HttpExchange exchange) {
        log.info("http request {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
        if (Objects.equals(exchange.getRequestMethod(), "GET") == false) {
            return;
        }
        final Drawing drawing = this.documentSupplier.get().drawing();
        writeDrawing(drawing, exchange);
    }

    private void handlePrevDrawingRequest(HttpExchange exchange) {
        log.info("http request {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
        if (Objects.equals(exchange.getRequestMethod(), "POST") == false) {
            return;
        }
        final Drawing drawing = this.documentSupplier.get().selectPreviousDrawing();
        writeDrawing(drawing, exchange);
        this.inboundMessagePublisher.submit(new RedrawMessage());
    }

    private void handleNextDrawingRequest(HttpExchange exchange) {
        log.info("http request {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
        if (Objects.equals(exchange.getRequestMethod(), "POST") == false) {
            return;
        }
        final Drawing drawing = this.documentSupplier.get().selectNextDrawing();
        writeDrawing(drawing, exchange);
        this.inboundMessagePublisher.submit(new RedrawMessage());
    }

    private void handleNewDrawingRequest(HttpExchange exchange) {
        log.info("http request {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
        if (Objects.equals(exchange.getRequestMethod(), "POST") == false) {
            return;
        }
        final Drawing drawing = this.documentSupplier.get().insertNewDrawing();
        writeDrawing(drawing, exchange);
        this.inboundMessagePublisher.submit(new RedrawMessage());
    }

    private void writeDrawing(Drawing drawing, HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            final OutputStream os = exchange.getResponseBody();
            final Module module = new SimpleModule().addSerializer(new CoordinateSerializer());
            final ObjectMapper mapper = new ObjectMapper().registerModule(module);
            final byte[] bytes = mapper.writeValueAsBytes(drawing);
            exchange.sendResponseHeaders(200, bytes.length);
            os.write(bytes);
            os.flush();
            os.close();
        } catch (IOException e) {
            log.error("error writing json response", e);
        }
    }

    private void handleInboundPacket(OSCPacket packet) {
        if (packet instanceof OSCBundle bundle) {
            log.info("bundle received @ {}: {}", bundle.getTimestamp(), bundle.getPackets());
            for (final OSCPacket innerPacket : bundle.getPackets()) {
                handleInboundPacket(innerPacket);
            }
            return;
        }
        if (packet instanceof OSCMessage oscMsg) {
            handleInboundMessage(oscMsg);
            return;
        }
        throw new IllegalArgumentException("invalid packet type: " + packet.getClass());
    }

    private void handleInboundMessage(OSCMessage oscMsg) {
        final List<Object> args = oscMsg.getArguments();
        log.info("message @ {}: {}", oscMsg.getAddress(), Joiner.on(", ").join(oscMsg.getArguments()));
        final Message message = switch (oscMsg.getAddress()) {
            case "/init/size" -> new InitSizeMessage((int) args.get(0), (int) args.get(1));
            case "/figure/begin" -> new FigureBeginMessage(
                    new Coordinate((float) args.get(0), (float) args.get(1)),
                    new Rgba((int) args.get(2), (int) args.get(3), (int) args.get(4), (int) args.get(5)),
                    (float) args.get(6));
            case "/figure/point" -> new FigurePointMessage(new Coordinate((float) args.get(0), (float) args.get(1)));
            case "/figure/end" -> new FigureEndMessage(new Coordinate((float) args.get(0), (float) args.get(1)));
            case "/figure/remove" -> new FigureRemoveMessage(
                    new Coordinate((float) args.get(0), (float) args.get(1)),
                    (int) args.get(2));
            default -> {
                log.warn("unrecognized OSC message address: {}", oscMsg.getAddress());
                yield null;
            }
        };
        if (message != null) {
            this.inboundMessagePublisher.submit(message);
        }
    }

    private class PacketListener implements OSCPacketListener {
        @Override
        public void handlePacket(OSCPacketEvent oscPacketEvent) {
            handleInboundPacket(oscPacketEvent.getPacket());
        }

        @Override
        public void handleBadData(OSCBadDataEvent oscBadDataEvent) {
            log.warn("bad osc data: {}", oscBadDataEvent);
        }
    }
}
