package eu.crushedpixel.replaymod.holders;

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
}
