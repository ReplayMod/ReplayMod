package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeComparator;
import eu.crushedpixel.replaymod.holders.Position;

import java.util.ArrayList;
import java.util.List;

public class KeyframeList<T extends Keyframe> extends ArrayList<T> {

    private static final KeyframeComparator KEYFRAME_COMPARATOR = new KeyframeComparator();

    @Override
    public boolean add(T t) {
        boolean success = super.add(t);
        sort();
        return success;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        sort();
    }

    @Override
    public T remove(int index) {
        T removed = super.remove(index);
        sort();
        return removed;
    }

    @Override
    public boolean remove(Object o) {
        boolean success = super.remove(o);
        sort();
        return success;
    }

    public void sort() {
        sort(KEYFRAME_COMPARATOR);
    }

    /**
     * Returns the first Keyframe that comes before a given value.
     * @param realTime The value to use
     * @return The first Keyframe prior to the given value
     */
    public T getPreviousKeyframe(int realTime) {
        if(this.isEmpty()) return null;

        T backup = null;

        List<T> found = new ArrayList<T>();

        for(T kf : this) {

            if(kf.getRealTimestamp() < realTime) {
                found.add(kf);
            } else if(kf.getRealTimestamp() == realTime) {
                backup = kf;
            }

        }

        if(found.size() > 0)
            return found.get(found.size() - 1); //last element is nearest

        else return backup;
    }

    /**
     * Returns the first Keyframe that comes after a given value.
     * @param realTime The value to use
     * @return The first Keyframe after the given value
     */
    public T getNextKeyframe(int realTime) {
        if(this.isEmpty()) return null;

        T backup = null;

        for(T kf : this) {

            if(kf.getRealTimestamp() > realTime) {
                return kf; //first found element is next
            } else if(kf.getRealTimestamp() == realTime) {
                backup = kf;
            }

        }

        return backup;
    }

    /**
     * Returns the Keyframe that is closest to the given timestamp.
     * @param realTime The timestamp to start searching at
     * @param tolerance The threshold to allow for close Keyframes
     * @return The closest Keyframe, or null if no Keyframe within treshold
     */
    public T getClosestKeyframeForTimestamp(int realTime, int tolerance) {
        List<T> found = new ArrayList<T>();
        for(T kf : this) {
            if(!(kf.getValue() instanceof Position)) continue;
            if(Math.abs(kf.getRealTimestamp() - realTime) <= tolerance) {
                found.add(kf);
            }
        }

        T closest = null;

        for(T kf : found) {
            if(closest == null || Math.abs(closest.getRealTimestamp() - realTime) > Math.abs(kf.getRealTimestamp() - realTime)) {
                closest = kf;
            }
        }
        return closest;
    }

    public T first() {
        if(isEmpty()) return null;
        return get(0);
    }

    public T last() {
        if(isEmpty()) return null;
        return get(size()-1);
    }

    /**
     * Returns a value between 0 and 1, representing the number that should be passed
     * to Interpolation#getValue() calls on this list of Keyframes.
     * @param timestamp The value to use
     * @return A value between 0 and 1
     */
    public float getPositionOnSpline(int timestamp) {
        Keyframe previousKeyframe = getPreviousKeyframe(timestamp);
        Keyframe nextKeyframe = getNextKeyframe(timestamp);

        int previousTimestamp = 0;
        int nextTimestamp = 0;

        if(nextKeyframe != null || previousKeyframe != null) {
            if(nextKeyframe != null) {
                nextTimestamp = nextKeyframe.getRealTimestamp();
            } else {
                nextTimestamp = previousKeyframe.getRealTimestamp();
            }

            if(previousKeyframe != null) {
                previousTimestamp = previousKeyframe.getRealTimestamp();
            } else {
                previousTimestamp = nextKeyframe.getRealTimestamp();
            }
        }

        int currentPosDiff = nextTimestamp - previousTimestamp;
        int currentPos = timestamp - previousTimestamp;

        float currentStepPercentage = (float) currentPos / (float) currentPosDiff;
        if(Float.isInfinite(currentStepPercentage)) currentStepPercentage = 0;

        float value = (indexOf(previousKeyframe) + currentStepPercentage) /
                (float)(size() - 1);

        return Math.max(0, Math.min(1, value));
    }

}
