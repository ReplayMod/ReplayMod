package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Transformation {

    /**
     * The object's anchor around which modifiers like orientation or position should be applied.
     * x=0, y=0, z=0 is the object's center.
     */
    private Position anchor;

    /**
     * The object's position in the Minecraft world.
     */
    private Position position;

    /**
     * The object's orientation.
     */
    private Position orientation;

    /**
     * The object's scale, individual values between 0 and 100.
     */
    private Position scale;

    /**
     * The object's opacity. Value between 0 and 100.
     */
    private double opacity;

}
