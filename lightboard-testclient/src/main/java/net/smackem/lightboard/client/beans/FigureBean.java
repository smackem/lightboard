package net.smackem.lightboard.client.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FigureBean {
    @JsonProperty private final List<PointBean> points = new ArrayList<>();

    @JsonCreator
    private FigureBean() {
    }

    public Collection<PointBean> points() {
        return this.points;
    }
}
