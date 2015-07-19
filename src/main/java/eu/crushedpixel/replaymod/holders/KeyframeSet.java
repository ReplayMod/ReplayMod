package eu.crushedpixel.replaymod.holders;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor

@EqualsAndHashCode
public class KeyframeSet implements GuiEntryListEntry {
    private String name;
    private Keyframe<AdvancedPosition>[] positionKeyframes;
    private Keyframe<TimestampValue>[] timeKeyframes;

    public KeyframeSet(String name, Keyframe[] keyframes) {
        this.name = name;
        setKeyframes(keyframes);
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public void setKeyframes(Keyframe[] keyframes) {
        List<Keyframe<AdvancedPosition>> posKFList = new ArrayList<Keyframe<AdvancedPosition>>();
        List<Keyframe<TimestampValue>> timeKFList = new ArrayList<Keyframe<TimestampValue>>();

        for(Keyframe kf : keyframes) {
            if(kf.getValue() instanceof AdvancedPosition)
                posKFList.add((Keyframe<AdvancedPosition>)kf);
            else if(kf.getValue() instanceof TimestampValue)
                timeKFList.add((Keyframe<TimestampValue>) kf);
        }

        positionKeyframes = posKFList.toArray(new Keyframe[posKFList.size()]);
        timeKeyframes = timeKFList.toArray(new Keyframe[timeKFList.size()]);
    }

    public String getName() {
        return name;
    }

    public Keyframe[] getKeyframes() {
        Keyframe[] keyframes = new Keyframe[positionKeyframes.length + timeKeyframes.length];
        System.arraycopy(positionKeyframes, 0, keyframes, 0, positionKeyframes.length);
        System.arraycopy(timeKeyframes, 0, keyframes, positionKeyframes.length, timeKeyframes.length);
        return keyframes;
    }

    public int getTimeKeyframeCount() {
        return timeKeyframes.length;
    }

    public int getPositionKeyframeCount() {
        return positionKeyframes.length;
    }

    public int getPathDuration() {
        int first = 0;
        int last = 0;

        for(Keyframe k : getKeyframes()) {
            if(k.getRealTimestamp() < first) {
                first = k.getRealTimestamp();
            } else if(k.getRealTimestamp() > last) {
                last = k.getRealTimestamp();
            }
        }

        return last-first;
    }

    @Override
    public String getDisplayString() {
        return name;
    }
}
