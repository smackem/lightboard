package net.smackem.lightboard.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;

class CoordinateSerializer extends StdSerializer<Coordinate> {

    CoordinateSerializer() {
        super(Coordinate.class);
    }

    @Override
    public void serialize(Coordinate coordinate, JsonGenerator json, SerializerProvider serializerProvider) throws IOException {
        json.writeStartObject();
        json.writeNumberField("x", coordinate.x);
        json.writeNumberField("y", coordinate.y);
        json.writeEndObject();
    }
}
