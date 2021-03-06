package net.smackem.lightboard.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class Rgba {
    @JsonProperty private final int r;
    @JsonProperty private final int g;
    @JsonProperty private final int b;
    @JsonProperty private final int a;

    @JsonCreator
    private Rgba() {
        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.a = 0;
    }

    public Rgba(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Rgba rgba = (Rgba) o;
        return r == rgba.r && g == rgba.g && b == rgba.b && a == rgba.a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b, a);
    }

    @Override
    public String toString() {
        return "Rgba{" +
               "r=" + r +
               ", g=" + g +
               ", b=" + b +
               ", a=" + a +
               '}';
    }
}
