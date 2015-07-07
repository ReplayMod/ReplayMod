package eu.crushedpixel.replaymod.holders;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExtendedPosition extends Position {

    public ExtendedPosition(double x, double y, double z, float width, float height) {
        super(x, y, z, 0, 0);
        this.width = width;
        this.height = height;
    }

    private float anchorX, anchorY;
    private float width, height;
    private float scale = 1f;
    private float opacity = 1f;

}
