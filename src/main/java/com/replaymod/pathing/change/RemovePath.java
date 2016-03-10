package com.replaymod.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Adds a new property.
 */
public final class RemovePath implements Change {
    @NonNull
    public static RemovePath create(Path path) {
        return new RemovePath(path.getTimeline().getPaths().indexOf(path));
    }

    RemovePath(int path) {
        this.path = path;
    }

    /**
     * Path index
     */
    private final int path;

    /**
     * The removed path
     */
    private volatile Path oldPath;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        oldPath = timeline.getPaths().remove(path);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        timeline.getPaths().add(path, oldPath);

        applied = false;
    }
}
