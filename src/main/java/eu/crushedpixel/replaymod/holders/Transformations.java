package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Transformations {

    private KeyframeList<Position> anchorKeyframes, positionKeyframes,
            orientationKeyframes, scaleKeyframes;
    private KeyframeList<NumberValue> opacityKeyframes;

    public Transformation getTransformationForTimestamp(int timestamp) {
        return null;
    }

}
