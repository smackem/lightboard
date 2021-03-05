package net.smackem.lightboard.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.smackem.lightboard.messaging.*;
import net.smackem.lightboard.model.Drawing;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

public class MessageExchangeHost implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MessageExchangeHost.class);
    public static final int DEFAULT_OSC_PORT = 7770;
    public static final int DEFAULT_HTTP_PORT = 7771;

    private final OSCPortIn inbound;
    private final SubmissionPublisher<Message> inboundMessagePublisher = new SubmissionPublisher<>();
    private final Supplier<Drawing> drawingSupplier;
    private final HttpServer httpServer;

    public MessageExchangeHost(Supplier<Drawing> drawingSupplier) throws IOException {
        this.drawingSupplier = Objects.requireNonNull(drawingSupplier);
        this.inbound = new OSCPortIn(DEFAULT_OSC_PORT);
        this.inbound.addPacketListener(new PacketListener());
        this.inbound.startListening();
        this.httpServer = HttpServer.create(new InetSocketAddress(DEFAULT_HTTP_PORT), 16);
        this.httpServer.createContext("/drawing", this::handleDrawingRequest);
        this.httpServer.start();
    }

    public Flow.Publisher<Message> inboundMessagePublisher() {
        return this.inboundMessagePublisher;
    }

    @Override
    public void close() throws Exception {
        this.inboundMessagePublisher.close();
        this.inbound.close();
        this.httpServer.stop(10);
    }

    private void handleDrawingRequest(HttpExchange exchange) {
        final Drawing drawing = this.drawingSupplier.get();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            new ObjectMapper().writeValueAsBytes(drawing);
        } catch (JsonProcessingException e) {
            log.error("error serializing drawing to json", e);
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
            case "/figure/begin" -> new FigureBeginMessage(new Coordinate((float) args.get(0), (float) args.get(1)));
            case "/figure/point" -> new FigurePointMessage(new Coordinate((float) args.get(0), (float) args.get(1)));
            case "/figure/end" -> new FigureEndMessage(new Coordinate((float) args.get(0), (float) args.get(1)));
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
            MessageExchangeHost.this.handleInboundPacket(oscPacketEvent.getPacket());
        }

        @Override
        public void handleBadData(OSCBadDataEvent oscBadDataEvent) {
            log.warn("bad osc data: {}", oscBadDataEvent);
        }
    }
}
