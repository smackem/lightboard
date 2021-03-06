package net.smackem.lightboard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Figure {
    private static final Logger log = LoggerFactory.getLogger(Figure.class);
    @JsonIgnore private final transient Object monitor = new Object();
    @JsonIgnore private final List<Coordinate> points = new ArrayList<>();
    @JsonProperty private final Rgba color;
    @JsonProperty private final double strokeWidth;

    public Figure(Rgba color, double strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    @JsonProperty
    public Collection<Coordinate> points() {
        synchronized (this.monitor) {
            return List.copyOf(this.points);
        }
    }

    public Rgba color() {
        return this.color;
    }

    public double strokeWidth() {
        return this.strokeWidth;
    }

    @JsonIgnore
    public boolean isEmpty() {
        synchronized (this.monitor) {
            return this.points.isEmpty();
        }
    }

    void addPoint(Coordinate point) {
        synchronized (this.monitor) {
            this.points.add(point);
        }
    }

    void simplify(double tolerance) {
        final int oldSize, newSize;
        synchronized (this.monitor) {
            final Geometry simplified = TopologyPreservingSimplifier.simplify(buildGeometry(), tolerance);
            oldSize = this.points.size();
            this.points.clear();
            this.points.addAll(List.of(simplified.getCoordinates()));
            newSize = this.points.size();
        }
        log.info("figure simplified: {} -> {} points", oldSize, newSize);
    }

    private Geometry buildGeometry() {
        final GeometryFactory gf = new GeometryFactory();
        return gf.createLineString(this.points.toArray(new Coordinate[0]));
    }
}
