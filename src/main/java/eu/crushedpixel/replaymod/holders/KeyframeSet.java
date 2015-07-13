package eu.crushedpixel.replaymod.holders;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
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
        List<Keyframe> kfList = new ArrayList<Keyframe>();
        Collections.addAll(kfList, positionKeyframes);
        Collections.addAll(kfList, timeKeyframes);
        return kfList.toArray(new Keyframe[kfList.size()]);
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

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof KeyframeSet)) return false;
        KeyframeSet set2 = (KeyframeSet)o2;
        return hashCode() == set2.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getKeyframes())
                .toHashCode();
    }
}
