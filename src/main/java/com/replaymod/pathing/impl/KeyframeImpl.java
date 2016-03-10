package com.replaymod.pathing.impl;

import com.google.common.base.Optional;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.property.Property;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class KeyframeImpl implements Keyframe {
    private final long time;
    private final Map<Property, Object> properties = new HashMap<>();

    @Override
    public long getTime() {
        return time;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getValue(Property<T> property) {
        return properties.containsKey(property) ? Optional.of((T) properties.get(property)) : Optional.<T>absent();
    }

    @Override
    public <T> void setValue(Property<T> property, T value) {
        properties.put(property, value);
    }

    @Override
    public void removeProperty(Property property) {
        properties.remove(property);
    }

    @Override
    public Set<Property> getProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }
}
