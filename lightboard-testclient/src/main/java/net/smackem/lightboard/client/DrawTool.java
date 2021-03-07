package net.smackem.lightboard.client;

public interface DrawTool {
    void onMousePressed(double x, double y);
    void onMouseDrag(double x, double y);
    void onMouseReleased(double x, double y);
}
