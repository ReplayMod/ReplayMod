package com.replaymod.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Adds a new property.
 */
public final class AddPath implements Change {
    @NonNull
    public static AddPath create() {
        return new AddPath();
    }

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        timeline.createPath();

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        timeline.getPaths().remove(timeline.getPaths().size() - 1);

        applied = false;
    }
}
