package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Keyframe<T extends KeyframeValue> {

    private int realTimestamp;
    private T value;

    public Keyframe<T> copy() {
        return new Keyframe<T>(realTimestamp, value);
    }
}
