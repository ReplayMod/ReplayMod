package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quaternion {

    private double w, x, y, z;

    public double dotProduct(Quaternion other) {
        return (this.getX() * other.getX() + this.getY() * other.getY() +
                this.getZ() * other.getZ() + this.getW() * other.getW());
    }
}
