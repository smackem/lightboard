package net.smackem.lightboard.messaging;

import net.smackem.lightboard.model.Rgba;
import org.locationtech.jts.geom.Coordinate;

public class FigureBeginMessage extends FigureMessage {
    private final Rgba color;
    private final double strokeWidth;

    public FigureBeginMessage(Coordinate point, Rgba color, double strokeWidth) {
        super(point);
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public Rgba color() {
        return this.color;
    }

    public double strokeWidth() {
        return this.strokeWidth;
    }
}
