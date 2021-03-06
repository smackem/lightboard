package net.smackem.lightboard.client.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RgbaBean {
    @JsonProperty private final int r;
    @JsonProperty private final int g;
    @JsonProperty private final int b;
    @JsonProperty private final int a;

    @JsonCreator
    private RgbaBean() {
        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.a = 0;
    }

    public int r() {
        return this.r;
    }

    public int g() {
        return this.g;
    }

    public int b() {
        return this.b;
    }

    public int a() {
        return this.a;
    }
}
