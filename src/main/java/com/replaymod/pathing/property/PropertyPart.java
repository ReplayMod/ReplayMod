package com.replaymod.pathing.property;

/**
 * Represents an (optionally) interpolatable part of a property (such as the X coordinate of the position property).
 * @param <T> Type of the property
 */
public interface PropertyPart<T> {
    Property<T> getProperty();

    /**
     * Returns whether this part should be interpolated between keyframes.
     * Examples would be x/y/z coordinates or sun location.
     * Counterexamples are spectated entity id or
     * @return {@code true} if this part should be interpolated, {@code false} otherwise
     */
    boolean isInterpolatable();

    /**
     * Convert this part of the value to a double for interpolation.
     * @param value The value, may be {@code null}
     * @return A double representing this value
     * @throws UnsupportedOperationException if this part is not interpolatable
     */
    double toDouble(T value);

    /**
     * Convert the specified double to this part of the value and return it combined with the value for other parts.
     * @param value Value of other parts
     * @param d Value for this part
     * @return Combined value
     * @throws UnsupportedOperationException if this part is not interpolatable
     */
    T fromDouble(T value, double d);
}
