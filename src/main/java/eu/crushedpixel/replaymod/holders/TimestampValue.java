package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class TimestampValue implements KeyframeValue {

    @Interpolate
    public double value;

    public int asInt() {
        return (int)value;
    }

    @Override
    public TimestampValue newInstance() {
        return new TimestampValue();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<TimestampValue>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<TimestampValue>();
    }
}
