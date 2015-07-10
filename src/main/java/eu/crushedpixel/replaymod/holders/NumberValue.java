package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.Interpolate;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
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

}
