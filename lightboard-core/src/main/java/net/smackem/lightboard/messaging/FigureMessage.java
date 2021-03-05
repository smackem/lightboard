package net.smackem.lightboard.messaging;

import org.locationtech.jts.geom.Coordinate;

import java.util.Objects;

public abstract class FigureMessage extends Message {
    private final Coordinate point;

    FigureMessage(Coordinate point) {
        this.point = Objects.requireNonNull(point);
    }

    public Coordinate point() {
        return point;
    }
}
