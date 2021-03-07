package net.smackem.lightboard.messaging;

import org.locationtech.jts.geom.Coordinate;

public class FigureRemoveMessage extends FigureMessage {
    private final int figureIndex;

    public FigureRemoveMessage(Coordinate point, int figureIndex) {
        super(point);
        this.figureIndex = figureIndex;
    }

    public int figureIndex() {
        return this.figureIndex;
    }
}
