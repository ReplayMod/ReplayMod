package com.replaymod.pathing.path;

import com.google.common.base.Optional;
import com.replaymod.pathing.property.Property;
import lombok.NonNull;

import java.util.Set;

/**
 * Represents a key frame in time that is used to compute frames in between multiple key frames.
 * Keyframes have different properties depending on the Interpolator tying them together.
 * One property tied by a different Interpolator to the previous one than to the next one inherits both properties.
 */
public interface Keyframe {

    /**
     * Return the time at which this property is set.
     *
     * @return Time in milliseconds since the start
     */
    long getTime();

    /**
     * Return the value of the property set at this property.
     *
     * @param property The property
     * @param <T>      Type of the property
     * @return Optional value of the property
     */
    @NonNull
    <T> Optional<T> getValue(Property<T> property);

    /**
     * Set the value for the property at this property.
     * If the property is not present, adds it.
     *
     * @param property The property
     * @param value    Value of the property, may be {@code null}
     * @param <T>      Type of the property
     */
    <T> void setValue(Property<T> property, T value);

    /**
     * Remove the specified property from this property.
     *
     * @param property The property to be removed
     */
    void removeProperty(Property property);

    /**
     * Returns all properties of this property
     * @return Set of properties
     */
    Set<Property> getProperties();
}
