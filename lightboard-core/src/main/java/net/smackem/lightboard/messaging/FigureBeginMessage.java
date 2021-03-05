package net.smackem.lightboard.messaging;

import org.locationtech.jts.geom.Coordinate;

public class FigureBeginMessage extends FigureMessage {
    public FigureBeginMessage(Coordinate point) {
        super(point);
    }
}
