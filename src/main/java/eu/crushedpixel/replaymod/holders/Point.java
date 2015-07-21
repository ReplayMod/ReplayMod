package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class Point implements KeyframeValue {

    @Interpolate
    public double x, y;

    @Override
    public Point newInstance() {
        return new Point();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<Point>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<Point>();
    }
}
