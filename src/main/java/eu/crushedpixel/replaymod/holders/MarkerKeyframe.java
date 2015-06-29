package eu.crushedpixel.replaymod.holders;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class MarkerKeyframe extends Keyframe {

    private Position position;
    private String name;

    public MarkerKeyframe(Position position, int timestamp, String name) {
        super(timestamp);
        this.position = position;
        this.name = name;
    }

    @Override
    public Keyframe copy() {
        return new MarkerKeyframe(position, getRealTimestamp(), name);
    }
}
