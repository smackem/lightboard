package net.smackem.lightboard.messaging;

import org.locationtech.jts.geom.Coordinate;

public class FigureEndMessage extends FigureMessage {
    public FigureEndMessage(Coordinate point) {
        super(point);
    }
}
