package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MarkerKeyframe extends Keyframe {

    private Position position;
    private String name;

    @Override
    public Keyframe clone() {
        return new MarkerKeyframe(this.getPosition(), this.getRealTimestamp(), this.getName());
    }

    public MarkerKeyframe(Position position, int timestamp, String name) {
        super(timestamp);
        this.position = position;
        this.name = name;
    }

    public Position getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof MarkerKeyframe)) return false;
        MarkerKeyframe m2 = (MarkerKeyframe)o2;
        return hashCode() == m2.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(position)
                .append(getRealTimestamp())
                .append(name)
                .toHashCode();
    }
}
