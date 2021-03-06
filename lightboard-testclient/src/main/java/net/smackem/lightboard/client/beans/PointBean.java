package net.smackem.lightboard.client.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class PointBean {
    @JsonProperty private final double x;
    @JsonProperty private final double y;

    @JsonCreator
    private PointBean() {
        this.x = 0;
        this.y = 0;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }
}
