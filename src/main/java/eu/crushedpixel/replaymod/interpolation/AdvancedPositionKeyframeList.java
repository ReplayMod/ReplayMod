package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.SpectatorData;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

@NoArgsConstructor
public class AdvancedPositionKeyframeList extends KeyframeList<AdvancedPosition> {

    private Set<SpectatorDataInterpolation> spectatorDataInterpolators;

    @Override
    public AdvancedPosition getInterpolatedValueForPathPosition(float pathPosition, boolean linear) {
        if(first() == null) return null;
        if(size() == 1) return first().getValue();

        @SuppressWarnings("unchecked")
        AdvancedPosition toApply = first().getValue().newInstance();

        if(previousCallLinear != (Boolean)linear) {
            recalculate(linear);
        }

        int keyframeIndex = (int)(pathPosition/size());
        float percentage = (pathPosition/size() - keyframeIndex)/(1f/size());
        AdvancedPosition current = get(keyframeIndex).getValue();

        SpectatorDataInterpolation dataInterpolation = null;
        if(current instanceof SpectatorData) {
            for(SpectatorDataInterpolation interpolation : spectatorDataInterpolators) {
                if(interpolation.contains((SpectatorData)current)) {
                    dataInterpolation = interpolation;
                    break;
                }
            }
        }

        if(dataInterpolation == null) {
            interpolation.applyPoint(pathPosition, toApply);
        } else {
            SpectatorData spectatorData = (SpectatorData)current;
            int index = dataInterpolation.indexOf(spectatorData);
            int size = dataInterpolation.size();
            float one = 1f/size;
            dataInterpolation.applyPoint((index/size)+(percentage*one), toApply);
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

        spectatorDataInterpolators = new HashSet<SpectatorDataInterpolation>();

        //find subsequent Spectator Keyframes with the same Entity ID
        ListIterator<Keyframe<AdvancedPosition>> iterator = this.listIterator();
        while(iterator.hasNext()) {
            Keyframe<AdvancedPosition> keyframe = iterator.next();
            SpectatorDataInterpolation spectatorInterpolation = null;

            boolean found = keyframe.getValue() instanceof SpectatorData;

            while(found) {
                if(spectatorInterpolation == null) {
                    spectatorInterpolation = new SpectatorDataInterpolation(linear);
                }
                spectatorInterpolation.addPoint((SpectatorData)keyframe.getValue(), keyframe.getRealTimestamp());

                if(iterator.hasNext()) {
                    keyframe = iterator.next();
                    found = keyframe.getValue() instanceof SpectatorData;
                } else {
                    found = false;
                }
            }

            if(spectatorInterpolation != null && spectatorInterpolation.size() > 1) {
                spectatorInterpolation.prepare();
                spectatorDataInterpolators.add(spectatorInterpolation);
            }
        }
    }
}
