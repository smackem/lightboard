package net.smackem.lightboard.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Document {
    private final Object monitor = new Object();
    private final List<Drawing> drawings = new LinkedList<>();
    private int drawingIndex;

    {
        this.drawings.add(new Drawing());
    }

    public Drawing drawing() {
        synchronized (this.monitor) {
            if (this.drawings.isEmpty()) {
                throw new IllegalStateException("there is no drawing");
            }
            Objects.checkIndex(this.drawingIndex, this.drawings.size());
            return this.drawings.get(this.drawingIndex);
        }
    }

    public Drawing selectPreviousDrawing() {
        synchronized (this.monitor) {
            if (this.drawings.isEmpty()) {
                throw new IllegalStateException("there is no drawing");
            }
            if (this.drawingIndex > 0) {
                this.drawingIndex--;
            }
            return this.drawings.get(this.drawingIndex);
        }
    }

    public Drawing selectNextDrawing() {
        synchronized (this.monitor) {
            if (this.drawings.isEmpty()) {
                throw new IllegalStateException("there is no drawing");
            }
            if (this.drawingIndex < this.drawings.size() - 1) {
                this.drawingIndex++;
            }
            return this.drawings.get(this.drawingIndex);
        }
    }

    public Drawing insertNewDrawing() {
        final Drawing drawing = new Drawing();
        synchronized (this.monitor) {
            Objects.checkIndex(this.drawingIndex, this.drawings.size());
            this.drawings.add(this.drawingIndex + 1, drawing);
            this.drawingIndex++;
        }
        return drawing;
    }
}
