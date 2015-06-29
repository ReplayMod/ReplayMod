package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyframeSet {
    private String name;
    private PositionKeyframe[] positionKeyframes;
    private TimeKeyframe[] timeKeyframes;

    public KeyframeSet(String name, Keyframe[] keyframes) {
        this.name = name;
        setKeyframes(keyframes);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKeyframes(Keyframe[] keyframes) {
        List<PositionKeyframe> posKFList = new ArrayList<PositionKeyframe>();
        List<TimeKeyframe> timeKFList = new ArrayList<TimeKeyframe>();

        for(Keyframe kf : keyframes) {
            if(kf instanceof PositionKeyframe)
                posKFList.add((PositionKeyframe)kf);
            else if(kf instanceof TimeKeyframe)
                timeKFList.add((TimeKeyframe) kf);
        }

        positionKeyframes = posKFList.toArray(new PositionKeyframe[posKFList.size()]);
        timeKeyframes = timeKFList.toArray(new TimeKeyframe[timeKFList.size()]);
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
    public String toString() {
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
