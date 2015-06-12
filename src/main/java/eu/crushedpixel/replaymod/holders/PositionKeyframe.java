package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PositionKeyframe extends Keyframe {

    private Position position;
    private Integer spectatedEntityID = null;

    @Override
    public Keyframe clone() {
        return new PositionKeyframe(getRealTimestamp(), position, spectatedEntityID);
    }

    public PositionKeyframe(int realTime, Position position) {
        super(realTime);
        this.position = position;
    }

    public PositionKeyframe(int realTime, Position position, Integer spectatedEntityID) {
        super(realTime);
        this.position = position;
        this.spectatedEntityID = spectatedEntityID;
    }

    public PositionKeyframe(int realTime, int spectatedEntityID) {
        this(realTime, new Position(spectatedEntityID), spectatedEntityID);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) { this.position = position; }

    public Integer getSpectatedEntityID() { return spectatedEntityID; }

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof PositionKeyframe)) return false;
        PositionKeyframe kf = (PositionKeyframe)o2;
        return hashCode() == kf.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getPosition())
                .append(getRealTimestamp())
                .append(getSpectatedEntityID())
                .toHashCode();
    }
}