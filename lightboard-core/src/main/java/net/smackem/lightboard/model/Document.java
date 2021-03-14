package net.smackem.lightboard.model;

import java.util.*;

public class Document {
    private final Object monitor = new Object();
    private final List<Drawing> drawings = new LinkedList<>();
    private int width;
    private int height;
    private int drawingIndex;

    {
        this.drawings.add(new Drawing());
    }

    public int width() {
        synchronized (this.monitor) {
            return this.width;
        }
    }

    public int height() {
        synchronized (this.monitor) {
            return this.height;
        }
    }

    public Collection<Drawing> drawings() {
        return Collections.unmodifiableCollection(this.drawings);
    }

    public void setSize(int width, int height) {
        synchronized (this.monitor) {
            this.width = width;
            this.height = height;
        }
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
