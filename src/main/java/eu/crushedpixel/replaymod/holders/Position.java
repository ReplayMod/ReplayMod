package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.util.BlockPos;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Position implements KeyframeValue {

    @Interpolate
    public double x, y, z;

    @Override
    public Position newInstance() {
        return new Position();
    }

    public Position(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<Position>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<Position>();
    }
}
