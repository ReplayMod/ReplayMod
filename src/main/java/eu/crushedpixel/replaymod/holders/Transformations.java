package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transformations {

    private KeyframeList<Position> anchorKeyframes = new KeyframeList<Position>();
    private KeyframeList<Position> positionKeyframes = new KeyframeList<Position>();
    private KeyframeList<Position> orientationKeyframes = new KeyframeList<Position>();
    private KeyframeList<Position> scaleKeyframes = new KeyframeList<Position>();
    private KeyframeList<NumberValue> opacityKeyframes = new KeyframeList<NumberValue>();

    public KeyframeList getKeyframeListByID(int id) {
        switch(id) {
            case 0:
                return anchorKeyframes;
            case 1:
                return positionKeyframes;
            case 2:
                return orientationKeyframes;
            case 3:
                return scaleKeyframes;
            case 4:
                return opacityKeyframes;
            default:
                return null;
        }
    }

    public Transformation getTransformationForTimestamp(int timestamp) {
        return new Transformation(
                anchorKeyframes.getInterpolatedValueForTimestamp(timestamp, true),
                positionKeyframes.getInterpolatedValueForTimestamp(timestamp, true),
                orientationKeyframes.getInterpolatedValueForTimestamp(timestamp, true),
                scaleKeyframes.getInterpolatedValueForTimestamp(timestamp, true),
                opacityKeyframes.getInterpolatedValueForTimestamp(timestamp, true).value);
    }

}
