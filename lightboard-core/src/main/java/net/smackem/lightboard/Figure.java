package net.smackem.lightboard;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Figure {
    private static final Logger log = LoggerFactory.getLogger(Figure.class);
    private final List<Coordinate> points = new ArrayList<>();

    public List<Coordinate> points() {
        return this.points;
    }

    public void simplify() {
        final Geometry simplified = TopologyPreservingSimplifier.simplify(buildGeometry(), 1.5);
        final int count = this.points.size();
        this.points.clear();
        this.points.addAll(List.of(simplified.getCoordinates()));
        log.info("figure simplified: {} -> {} points", count, this.points.size());
    }

    private Geometry buildGeometry() {
        final GeometryFactory gf = new GeometryFactory();
        return gf.createLineString(this.points.toArray(new Coordinate[0]));
    }
}
