package net.smackem.lightboard.messaging;

import org.locationtech.jts.geom.Coordinate;

public class FigurePointMessage extends FigureMessage {
    public FigurePointMessage(Coordinate point) {
        super(point);
    }
}
