package com.replaymod.pathing.change;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.replaymod.pathing.interpolation.Interpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import lombok.NonNull;

/**
 * Removes a property.
 */
public final class RemoveKeyframe implements Change {
    @NonNull
    public static RemoveKeyframe create(@NonNull Path path, @NonNull Keyframe keyframe) {
        return new RemoveKeyframe(path.getTimeline().getPaths().indexOf(path),
                Iterables.indexOf(path.getKeyframes(), Predicates.equalTo(keyframe)));
    }

    RemoveKeyframe(int path, int index) {
        this.path = path;
        this.index = index;
    }

    /**
     * Path index
     */
    private final int path;

    /**
     * Index of the property to be removed.
     */
    private final int index;

    private volatile Keyframe removedKeyframe;
    private volatile Interpolator removedInterpolator;

    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        Path path = timeline.getPaths().get(this.path);
        if (index < path.getSegments().size()) {
            removedInterpolator = Iterables.get(path.getSegments(), index).getInterpolator();
        }
        path.remove(removedKeyframe = Iterables.get(path.getKeyframes(), index), true);

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        Path path = timeline.getPaths().get(this.path);
        path.insert(removedKeyframe);
        if (removedInterpolator != null) {
            Iterables.get(path.getSegments(), index).setInterpolator(removedInterpolator);
        }

        applied = false;
    }
}
