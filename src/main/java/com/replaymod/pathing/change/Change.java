package com.replaymod.pathing.change;

import com.replaymod.pathing.path.Timeline;

/**
 * A change to any part of a timeline.
 * If {@link #undo(Timeline)} is not called in the reverse order of {@link #apply(Timeline)}, the behavior is unspecified.
 * Instances implementing this interfaces must not reference any outside objects required for applying
 * as they might be serialized and deserialized at any time.
 * After deserialization, only the {@link #apply(Timeline)} method is guaranteed to work.
 */
public interface Change {

    /**
     * Apply this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If already applied.
     */
    void apply(Timeline timeline);

    /**
     * Undo this change.
     *
     * @param timeline The timeline
     * @throws IllegalStateException If not yet applied.
     */
    void undo(Timeline timeline);
}
