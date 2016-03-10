package com.replaymod.pathing.path;

import com.replaymod.pathing.interpolation.Interpolator;
import lombok.NonNull;

/**
 * Represents a segment of a Path consisting of one start property and one end property.
 * Each segment is interpolated by one Interpolator. Multiple segments may have the same Interpolator and can only
 * be interpolated together (they may or may not influence each other indirectly).
 */
public interface PathSegment {
    /**
     * Return the start property of this segment.
     *
     * @return The first property
     */
    @NonNull
    Keyframe getStartKeyframe();

    /**
     * Return the end property of this segment.
     *
     * @return The second property
     */
    @NonNull
    Keyframe getEndKeyframe();

    /**
     * Return the interpolator responsible for this path segment.
     *
     * @return The interpolator or {@code null} if not yet set
     * @throws IllegalStateException If no interpolator has been set yet
     */
    @NonNull
    Interpolator getInterpolator();

    /**
     * Set the interpolator responsible for this path segment.
     * @param interpolator The interpolator
     */
    void setInterpolator(Interpolator interpolator);
}
