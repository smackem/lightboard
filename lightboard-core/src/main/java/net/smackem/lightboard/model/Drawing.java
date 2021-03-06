package net.smackem.lightboard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Drawing {
    @JsonIgnore private final transient Object monitor = new Object();
    @JsonIgnore private final List<Figure> figures = new ArrayList<>();

    @JsonProperty
    public Collection<Figure> figures() {
        synchronized (this.monitor) {
            return List.copyOf(this.figures);
        }
    }

    @JsonIgnore
    public boolean isBlank() {
        synchronized (this.monitor) {
            return this.figures.isEmpty();
        }
    }

    public void clear() {
        synchronized (this.monitor) {
            this.figures.clear();
        }
    }

    public void beginFigure(Coordinate point, Rgba color, double strokeWidth) {
        synchronized (this.monitor) {
            final Figure figure = currentFigure();
            if (figure != null && figure.isEmpty()) {
                this.figures.remove(this.figures.size() - 1);
            }
            final Figure newFigure = new Figure(color, strokeWidth);
            newFigure.addPoint(point);
            this.figures.add(newFigure);
        }
    }

    public void addPoint(Coordinate point) {
        synchronized (this.monitor) {
            final Figure figure = currentFigure();
            if (figure == null) {
                return;
            }
            figure.addPoint(point);
        }
    }

    public void endFigure(Coordinate point, double simplificationTolerance) {
        synchronized (this.monitor) {
            final Figure figure = currentFigure();
            if (figure == null) {
                return;
            }
            figure.addPoint(point);
            figure.simplify(simplificationTolerance);
        }
    }

    public Figure currentFigure() {
        synchronized (this.monitor) {
            return this.figures.size() > 0
                    ? this.figures.get(this.figures.size() - 1)
                    : null;
        }
    }

    public Figure removeFigure(int figureIndex) {
        synchronized (this.monitor) {
            Objects.checkIndex(figureIndex, this.figures.size());
            return this.figures.remove(figureIndex);
        }
    }
}
