package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.Interpolate;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
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

}
