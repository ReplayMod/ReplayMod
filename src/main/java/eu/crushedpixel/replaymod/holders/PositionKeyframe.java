package eu.crushedpixel.replaymod.holders;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public final class PositionKeyframe extends Keyframe {

    private Position position;
    private Integer spectatedEntityID;

    public PositionKeyframe(int realTime, Position position) {
        this(realTime, position, null);
    }

    public PositionKeyframe(int realTime, Position position, Integer spectatedEntityID) {
        super(realTime);
        this.position = position;
        this.spectatedEntityID = spectatedEntityID;
    }

    public PositionKeyframe(int realTime, int spectatedEntityID) {
        this(realTime, new Position(spectatedEntityID), spectatedEntityID);
    }

    @Override
    public Keyframe copy() {
        return new PositionKeyframe(getRealTimestamp(), position, spectatedEntityID);
    }
}