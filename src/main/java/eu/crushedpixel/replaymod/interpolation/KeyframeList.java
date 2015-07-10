package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyframeList<K extends KeyframeValue> extends ArrayList<Keyframe<K>> {

    private static final KeyframeComparator KEYFRAME_COMPARATOR = new KeyframeComparator();

    private Boolean previousCallLinear = null;

    private Interpolation<K> interpolation;

    @Override
    public boolean add(Keyframe<K> t) {
        boolean success = super.add(t);
        sort();
        return success;
    }

    @Override
    public void add(int index, Keyframe<K> element) {
        super.add(index, element);
        sort();
    }

    @Override
    public Keyframe<K> remove(int index) {
        Keyframe<K> removed = super.remove(index);
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
        previousCallLinear = null;
        Collections.sort(this, KEYFRAME_COMPARATOR);
    }

    /**
     * Returns the first Keyframe that comes before a given value.
     * @param realTime The value to use
     * @return The first Keyframe prior to the given value
     */
    public Keyframe<K> getPreviousKeyframe(int realTime) {
        if(this.isEmpty()) return null;

        Keyframe<K> backup = null;

        List<Keyframe<K>> found = new ArrayList<Keyframe<K>>();

        for(Keyframe<K> kf : this) {

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
    public Keyframe<K> getNextKeyframe(int realTime) {
        if(this.isEmpty()) return null;

        Keyframe<K> backup = null;

        for(Keyframe<K> kf : this) {

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
    public Keyframe<K> getClosestKeyframeForTimestamp(int realTime, int tolerance) {
        List<Keyframe<K>> found = new ArrayList<Keyframe<K>>();
        for(Keyframe<K> kf : this) {
            if(!(kf.getValue() instanceof AdvancedPosition)) continue;
            if(Math.abs(kf.getRealTimestamp() - realTime) <= tolerance) {
                found.add(kf);
            }
        }

        Keyframe<K> closest = null;

        for(Keyframe<K> kf : found) {
            if(closest == null || Math.abs(closest.getRealTimestamp() - realTime) > Math.abs(kf.getRealTimestamp() - realTime)) {
                closest = kf;
            }
        }
        return closest;
    }

    public Keyframe<K> first() {
        if(isEmpty()) return null;
        return get(0);
    }

    public Keyframe<K> last() {
        if(isEmpty()) return null;
        return get(size()-1);
    }

    public K getInterpolatedValueForTimestamp(int timestamp, boolean linear) {
        return getInterpolatedValueForPathPosition(getPositionOnPath(timestamp), linear);
    }

    public K getInterpolatedValueForPathPosition(float pathPosition, boolean linear) {
        //as every implementation of KeyframeValue returns a new instance of itself, this warning can be ignored
        @SuppressWarnings("unchecked")
        K toApply = (K)first().getValue().newInstance();

        if(previousCallLinear != (Boolean)linear) {
            interpolation = linear ? new GenericLinearInterpolation<K>() : new GenericSplineInterpolation<K>();

            for(Keyframe<K> keyframe : this) {
                interpolation.addPoint(keyframe.getValue());
            }

            interpolation.prepare();
        }

        interpolation.applyPoint(pathPosition, toApply);

        return toApply;
    }

    /**
     * Returns a value between 0 and 1, representing the number that should be passed
     * to Interpolation#getValue() calls on this list of Keyframes.
     * @param timestamp The value to use
     * @return A value between 0 and 1
     */
    private float getPositionOnPath(int timestamp) {
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
