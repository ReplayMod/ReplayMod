package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Transformation {

    private Position anchor;
    private Position position;
    private Position orientation;
    private Position scale;
    private double opacity;

    private float width, height;

}
