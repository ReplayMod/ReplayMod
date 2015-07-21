package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class NumberValue implements KeyframeValue {

    @Interpolate
    public double value;

    @Override
    public NumberValue newInstance() {
        return new NumberValue();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<NumberValue>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<NumberValue>();
    }

}
