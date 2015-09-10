package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.SpectatorData;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import lombok.NoArgsConstructor;

import java.util.ListIterator;

@NoArgsConstructor
public class AdvancedPositionKeyframeList extends KeyframeList<AdvancedPosition> {

    private KeyframeList<AdvancedPosition> completedKeyframes;

    @Override
    public AdvancedPosition getInterpolatedValueForPathPosition(float pathPosition, boolean linear) {
        if(first() == null) return null;
        if(size() == 1) return first().getValue();

        if(previousCallLinear != (Boolean)linear) {
            recalculate(linear);
        }

        //convert the path position to match the completedKeyframes list
        int timestamp = getTimestampFromPathPosition(pathPosition);
        float newPathPosition = completedKeyframes.getPositionOnPath(timestamp);
        return completedKeyframes.getInterpolatedValueForPathPosition(newPathPosition, linear);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void recalculate(boolean linear) {
        previousCallLinear = linear;

        if(size() < 2) return;

        completedKeyframes = new KeyframeList<AdvancedPosition>();

        //find subsequent Spectator Keyframes with the same Entity ID
        ListIterator<Keyframe<AdvancedPosition>> iterator = this.listIterator();
        while(iterator.hasNext()) {
            Keyframe<AdvancedPosition> keyframe = iterator.next();
            SpectatorDataInterpolation spectatorInterpolation = null;

            boolean found = keyframe.getValue() instanceof SpectatorData;
            int firstTimestamp = -1;

            if(!found) {
                completedKeyframes.add(keyframe);
            }

            while(found) {
                if(spectatorInterpolation == null) {
                    spectatorInterpolation = new SpectatorDataInterpolation(linear);
                }
                int keyframeTimestamp = keyframe.getRealTimestamp();
                TimestampValue timestampValue = ReplayHandler.getTimeKeyframes().getInterpolatedValueForTimestamp(keyframeTimestamp, true);
                int replayTimestamp = timestampValue == null ? 0 : timestampValue.asInt();
                if(firstTimestamp == -1) firstTimestamp = replayTimestamp;
                spectatorInterpolation.addPoint(keyframe, replayTimestamp);
                if(iterator.hasNext()) {
                    keyframe = iterator.next();
                    found = keyframe.getValue() instanceof SpectatorData;
                    if(!found) completedKeyframes.add(keyframe);
                } else {
                    found = false;
                }
            }

            if(spectatorInterpolation != null && spectatorInterpolation.size() > 1) {
                spectatorInterpolation.prepare();
                completedKeyframes.addAll(spectatorInterpolation.elements());
            }
        }
    }
}
