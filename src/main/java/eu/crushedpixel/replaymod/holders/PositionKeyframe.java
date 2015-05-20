package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PositionKeyframe extends Keyframe {

    private Position position;

    @Override
    public Object clone() {
        return new PositionKeyframe(this.getRealTimestamp(), this.getPosition());
    }

    public PositionKeyframe(int realTime, Position position) {
        super(realTime);
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) { this.position = position; }

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
                .toHashCode();
    }
}