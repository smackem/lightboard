package net.smackem.lightboard.client.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DrawingBean {
    @JsonProperty private final List<FigureBean> figures = new ArrayList<>();

    @JsonCreator
    private DrawingBean() {
    }

    public Collection<FigureBean> figures() {
        return this.figures;
    }
}
