package eu.crushedpixel.replaymod.holders;

public class Keyframe {

    private final int realTimestamp;

    public Keyframe(int realTimestamp) {
        this.realTimestamp = realTimestamp;
    }

    public int getRealTimestamp() {
        return realTimestamp;
    }
}
