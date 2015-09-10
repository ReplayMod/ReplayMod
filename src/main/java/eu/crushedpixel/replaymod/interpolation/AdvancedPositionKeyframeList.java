package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.SpectatorData;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.Map.Entry;

@NoArgsConstructor
public class AdvancedPositionKeyframeList extends KeyframeList<AdvancedPosition> {

    private Map<List<Integer>, SpectatorDataInterpolation> spectatorDataInterpolators;

    @Override
    public AdvancedPosition getInterpolatedValueForPathPosition(float pathPosition, boolean linear) {
        if(first() == null) return null;
        if(size() == 1) return first().getValue();

        @SuppressWarnings("unchecked")
        AdvancedPosition toApply = first().getValue().newInstance();

        if(previousCallLinear != (Boolean)linear) {
            recalculate(linear);
        }

        int keyframeIndex = (int)Math.min(size()-1, pathPosition*(size()-1));
        float remainder = (pathPosition - ((float)keyframeIndex/(size()-1)));
        float partial = remainder / (1f/(size()-1));

        AdvancedPosition current = get(keyframeIndex).getValue();

        SpectatorDataInterpolation dataInterpolation = null;
        if(current instanceof SpectatorData) {
            for(Entry<List<Integer>, SpectatorDataInterpolation> entry : spectatorDataInterpolators.entrySet()) {
                if(entry.getKey().contains(keyframeIndex)) dataInterpolation = entry.getValue();
            }
        }

        if(dataInterpolation == null) {
            interpolation.applyPoint(pathPosition, toApply);
        } else {
            SpectatorData spectatorData = (SpectatorData)current;
            int index = dataInterpolation.indexOf(spectatorData);
            int size = dataInterpolation.size()-1;
            float one = 1f/size;
            float pathPos = (index/(float)size)+(partial*one);
            dataInterpolation.applyPoint(pathPos, toApply);
        }

        return toApply;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void recalculate(boolean linear) {
        previousCallLinear = linear;

        if(size() < 2) return;

        interpolation = linear ? first().getValue().getLinearInterpolator() : first().getValue().getCubicInterpolator();

        for(Keyframe<AdvancedPosition> keyframe : this) {
            interpolation.addPoint(keyframe.getValue());
        }

        interpolation.prepare();

        spectatorDataInterpolators = new HashMap<List<Integer>, SpectatorDataInterpolation>();

        //find subsequent Spectator Keyframes with the same Entity ID
        ListIterator<Keyframe<AdvancedPosition>> iterator = this.listIterator();
        while(iterator.hasNext()) {
            Keyframe<AdvancedPosition> keyframe = iterator.next();
            SpectatorDataInterpolation spectatorInterpolation = null;

            boolean found = keyframe.getValue() instanceof SpectatorData;
            int first = iterator.nextIndex()-1;
            int firstTimestamp = -1;

            while(found) {
                if(spectatorInterpolation == null) {
                    spectatorInterpolation = new SpectatorDataInterpolation(linear);
                }
                int keyframeTimestamp = keyframe.getRealTimestamp();
                TimestampValue timestampValue = ReplayHandler.getTimeKeyframes().getInterpolatedValueForTimestamp(keyframeTimestamp, true);
                int replayTimestamp = timestampValue == null ? 0 : timestampValue.asInt();
                if(firstTimestamp == -1) firstTimestamp = replayTimestamp;
                spectatorInterpolation.addPoint((SpectatorData)keyframe.getValue(), replayTimestamp);
                if(iterator.hasNext()) {
                    keyframe = iterator.next();
                    found = keyframe.getValue() instanceof SpectatorData;
                } else {
                    found = false;
                }
            }

            if(spectatorInterpolation != null && spectatorInterpolation.size() > 1) {
                spectatorInterpolation.prepare();
                List<Integer> keys = new ArrayList<Integer>();
                for(int i=first; i<first+spectatorInterpolation.size()-1; i++) {
                    keys.add(i);
                }
                spectatorDataInterpolators.put(keys, spectatorInterpolation);
            }
        }
    }
}
