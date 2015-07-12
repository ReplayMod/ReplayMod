package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transformations {

    private Position defaultAnchor = new Position(0, 0, 0),
            defaultPosition = new Position(0, 0, 0),
            defaultOrientation = new Position(0, 0, 0),
            defaultScale = new Position(100, 100, 100);
    private NumberValue defaultOpacity = new NumberValue(100);

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
        Position anchor = anchorKeyframes.getInterpolatedValueForTimestamp(timestamp, true);
        if(anchor == null) anchor = defaultAnchor;

        Position position = positionKeyframes.getInterpolatedValueForTimestamp(timestamp, true);
        if(position == null) position = defaultPosition;

        Position orientation = orientationKeyframes.getInterpolatedValueForTimestamp(timestamp, true);
        if(orientation == null) orientation = defaultOrientation;

        Position scale = scaleKeyframes.getInterpolatedValueForTimestamp(timestamp, true);
        if(scale == null) scale = defaultScale;

        NumberValue opacity = opacityKeyframes.getInterpolatedValueForTimestamp(timestamp, true);
        if(opacity == null) opacity = defaultOpacity;

        return new Transformation(anchor, position, orientation, scale, opacity.value);
    }

}
