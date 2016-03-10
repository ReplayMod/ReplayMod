package com.replaymod.pathing.interpolation;

import com.google.common.base.Optional;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.PathSegment;
import com.replaymod.pathing.property.Property;
import com.replaymod.pathing.property.PropertyPart;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interpolates multiple keyframes. Also provides all Properties it can handle.
 * Each Interpolator may provide certain outputs for the next Interpolator.
 * Interpolators are evaluated first to last.
 */
public interface Interpolator {
    /**
     * Returns a collection of all properties applicable for this Interpolator.
     *
     * @return Collection of properties or empty collection if none
     */
    @NonNull
    Collection<Property> getKeyframeProperties();

    /**
     * Add the specified path segment to this interpolator.
     * <p/>
     * This method should be called by the PathSegment itself.
     *
     * @param segment The path segments
     */
    void addSegment(PathSegment segment);

    /**
     * Remove the specified path segment from this interpolator.
     * <p/>
     * This method should be called by the PathSegment itself.
     *
     * @param segment The path segments
     */
    void removeSegment(PathSegment segment);

    /**
     * Returns an immutable list of all path segments handled by this interpolator.
     * Ordering is only guaranteed after {@link #bake(Map)} has been called and only until
     * the list is modified.
     *
     * @return List of path segments or empty list if none
     */
    @NonNull
    List<PathSegment> getSegments();

    /**
     * Bake the interpolation of the current path segments with the specified parameters.
     * Note that when this method is called directly, the path might need to be updated via {@link Path#update()}
     *
     * @param parameters Map of parameters for some properties
     * @return Map of parameters for the next interpolator
     * @throws IllegalStateException If no segments have been set yet
     *                               or the segments are not continuous
     */
    @NonNull
    Map<PropertyPart, InterpolationParameters> bake(Map<PropertyPart, InterpolationParameters> parameters);

    /**
     * Returns whether the segments handled by this interpolator have changed since the last
     * call of {@link #bake(Map)}.
     * This only includes the segments themselves, not the properties of their keyframes, those
     * have to be tracked manually.
     * @return {@code true} if segments have changed, {@code false} otherwise
     */
    boolean isDirty();

    /**
     * Return the value of the property at the specified point in time.
     *
     * @param property The property
     * @param time     Time in milliseconds since the start
     * @param <T>      Type of the property
     * @return Optional value of the property
     * @throws IllegalStateException If {@link #bake(Map)} has not yet been called
     *                               or {@link #addSegment(PathSegment)}/{@link #removeSegment(PathSegment)}
     *                               has been changed since the last bake
     */
    <T> Optional<T> getValue(Property<T> property, long time);
}
