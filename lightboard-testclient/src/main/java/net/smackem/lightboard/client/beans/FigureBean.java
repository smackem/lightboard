package net.smackem.lightboard.client.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FigureBean {
    @JsonProperty private final List<PointBean> points = new ArrayList<>();
    @JsonProperty private final double strokeWidth;
    @JsonProperty private final RgbaBean color;

    @JsonCreator
    private FigureBean() {
        this.strokeWidth = 0;
        this.color = null;
    }

    public Collection<PointBean> points() {
        return this.points;
    }

    public double strokeWidth() {
        return this.strokeWidth;
    }

    public RgbaBean color() {
        return this.color;
    }
}
