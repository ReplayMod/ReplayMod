package eu.crushedpixel.replaymod.holders;

public class TimeKeyframe extends Keyframe {

    private final int timestamp;

    public TimeKeyframe(int realTime, int timestamp) {
        super(realTime);
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
