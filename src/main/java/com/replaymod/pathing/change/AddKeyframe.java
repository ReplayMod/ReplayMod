package com.replaymod.pathing.change;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Adds a new property.
 */
public final class AddKeyframe implements Change {
    @NonNull
    public static AddKeyframe create(Path path, long time) {
        return new AddKeyframe(path.getTimeline().getPaths().indexOf(path), time);
    }

    AddKeyframe(int path, long time) {
        this.path = path;
        this.time = time;
    }

    /**
     * Path index
     */
    private final int path;

    /**
     * Time at which the property should be injected.
     */
    private final long time;

    /**
     * Index of the newly created property.
     */
    private int index;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        Keyframe keyframe = path.insert(time);
        index = Iterables.indexOf(path.getKeyframes(), Predicates.equalTo(keyframe));

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        path.remove(Iterables.get(path.getKeyframes(), index), true);

        applied = false;
    }
}
