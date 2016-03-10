package com.replaymod.pathing.property;

public abstract class AbstractPropertyPart<T> implements PropertyPart<T> {
    private final Property<T> property;
    private final boolean interpolatable;

    public AbstractPropertyPart(Property<T> property, boolean interpolatable) {
        this.property = property;
        this.interpolatable = interpolatable;
    }

    @Override
    public Property<T> getProperty() {
        return property;
    }

    @Override
    public boolean isInterpolatable() {
        return interpolatable;
    }
}
