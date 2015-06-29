package eu.crushedpixel.replaymod.holders;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class TimeKeyframe extends Keyframe {

    private final int timestamp;

    public TimeKeyframe(int realTime, int timestamp) {
        super(realTime);
        this.timestamp = timestamp;
    }

    @Override
    public Keyframe copy() {
        return new TimeKeyframe(getRealTimestamp(), getTimestamp());
    }
}
