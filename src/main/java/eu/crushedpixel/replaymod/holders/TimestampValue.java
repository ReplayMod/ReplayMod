package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.Interpolate;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class TimestampValue extends KeyframeValue {

    @Interpolate
    public double value;

}
