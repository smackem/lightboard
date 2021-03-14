package net.smackem.lightboard.io;

import net.smackem.lightboard.model.Document;
import net.smackem.lightboard.model.Drawing;
import net.smackem.lightboard.model.Figure;
import net.smackem.lightboard.model.Rgba;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

public class SvgWriter {
    public void write(Document document, Appendable out) throws IOException {
        final Collection<Drawing> drawings = document.drawings();
        int drawingIndex = 0;
        int baseY = 0;
        out.append(String.format(Locale.ROOT, """
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d">
                """, document.width(), document.height() * drawings.size()));
        for (final Drawing drawing : document.drawings()) {
            if ((drawingIndex & 1) == 1) {
                out.append(String.format(Locale.ROOT, """
                        <rect x="0" y="%d" width="%d" height="%d"
                              style="fill:#e0e0e0" />
                        """, baseY, document.width(), document.height()));
            }
            writeDrawing(drawing, baseY, out);
            baseY += document.height();
            drawingIndex++;
        }
        out.append("</svg>\n");
    }

    private static void writeDrawing(Drawing drawing, int baseY, Appendable out) throws IOException {
        for (final Figure figure : drawing.figures()) {
            final String points = figure.points().stream()
                    .map(pt -> String.format(Locale.ROOT, "%f,%f", pt.x, pt.y + baseY))
                    .collect(Collectors.joining(" "));
            out.append(String.format(Locale.ROOT, """
                    <polyline points="%s"
                              style="fill:none;stroke:%s;stroke-width:%f" />
                    """, points, toWeb(figure.color()), figure.strokeWidth()));
        }
    }

    private static String toWeb(Rgba rgba) {
        return String.format(Locale.ROOT,
                "rgba(%d, %d, %d, %f)",
                rgba.r(), rgba.g(), rgba.b(), (float) rgba.a() / 255.0f);
    }
}
