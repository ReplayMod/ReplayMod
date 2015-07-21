package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.GenericLinearInterpolation;
import eu.crushedpixel.replaymod.interpolation.GenericSplineInterpolation;
import eu.crushedpixel.replaymod.interpolation.Interpolation;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Marker implements KeyframeValue {

    private String name;
    private AdvancedPosition position;

    @Override
    public KeyframeValue newInstance() {
        return new Marker();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<Marker>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<Marker>();
    }
}
