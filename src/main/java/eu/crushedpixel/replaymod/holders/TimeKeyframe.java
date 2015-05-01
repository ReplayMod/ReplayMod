package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TimeKeyframe extends Keyframe {

    private final int timestamp;

    public TimeKeyframe(int realTime, int timestamp) {
        super(realTime);
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof TimeKeyframe)) return false;
        TimeKeyframe kf = (TimeKeyframe)o2;
        return hashCode() == kf.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getTimestamp())
                .append(getRealTimestamp())
                .toHashCode();
    }
}
