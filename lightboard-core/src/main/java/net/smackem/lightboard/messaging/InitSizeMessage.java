package net.smackem.lightboard.messaging;

public class InitSizeMessage extends InitMessage {
    private final int width;
    private final int height;

    public InitSizeMessage(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
