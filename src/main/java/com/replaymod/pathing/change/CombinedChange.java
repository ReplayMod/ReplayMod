package com.replaymod.pathing.change;

import com.google.common.base.Preconditions;
import com.replaymod.pathing.path.Timeline;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Represents multiple changes as one change.
 */
public class CombinedChange implements Change {

    /**
     * Combines the specified changes into one change in the order given.
     * All changes must not yet have been applied.
     *
     * @param changes List of changes
     * @return A new CombinedChange instance
     */
    @NonNull
    public static CombinedChange create(Change... changes) {
        return new CombinedChange(Arrays.asList(changes), false);
    }

    /**
     * Combines the specified changes into one change in the order given.
     * All changes must have been applied.
     *
     * @param changes List of changes
     * @return A new CombinedChange instance
     */
    @NonNull
    public static CombinedChange createFromApplied(Change... changes) {
        return new CombinedChange(Arrays.asList(changes), true);
    }

    CombinedChange(List<Change> changeList, boolean applied) {
        this.changeList = changeList;
        this.applied = applied;
    }

    private final List<Change> changeList;
    private boolean applied;

    @Override
    public void apply(Timeline timeline) {
        Preconditions.checkState(!applied, "Already applied!");

        for (Change change : changeList) {
            change.apply(timeline);
        }

        applied = true;
    }

    @Override
    public void undo(Timeline timeline) {
        Preconditions.checkState(applied, "Not yet applied!");

        ListIterator<Change> iterator = changeList.listIterator(changeList.size());
        while (iterator.hasPrevious()) {
            iterator.previous().undo(timeline);
        }

        applied = false;
    }
}
