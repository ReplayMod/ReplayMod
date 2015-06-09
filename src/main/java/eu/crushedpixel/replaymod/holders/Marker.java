package eu.crushedpixel.replaymod.holders;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Marker {

    public Marker(Position position, int timestamp, String name) {
        this.position = position;
        this.timestamp = timestamp;
        this.name = name;
    }

    private Position position;
    private int timestamp;
    private String name;

    public Position getPosition() {
        return position;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof Marker)) return false;
        Marker m2 = (Marker)o2;
        return hashCode() == m2.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(position)
                .append(timestamp)
                .append(name)
                .toHashCode();
    }
}
