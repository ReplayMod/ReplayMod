package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.*;

@Data
@AllArgsConstructor
public class Transformations {

    private Position anchor;
    private Position position;
    private Position orientation;
    private Point scale;
    private float opacity;

    private float width, height;

}
