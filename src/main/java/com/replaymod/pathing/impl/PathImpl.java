package com.replaymod.pathing.impl;

import com.google.common.base.Optional;
import com.replaymod.pathing.interpolation.InterpolationParameters;
import com.replaymod.pathing.interpolation.Interpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.PathSegment;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.property.Property;
import com.replaymod.pathing.property.PropertyPart;

import java.util.*;

public class PathImpl implements Path {
    private final Timeline timeline;
    private Map<Long, Keyframe> keyframes = new TreeMap<>();
    private List<PathSegment> segments = new LinkedList<>();
    private boolean active = true;

    public PathImpl(Timeline timeline) {
        this.timeline = timeline;
    }

    @Override
    public Timeline getTimeline() {
        return timeline;
    }

    @Override
    public Collection<Keyframe> getKeyframes() {
        return Collections.unmodifiableCollection(keyframes.values());
    }

    @Override
    public Collection<PathSegment> getSegments() {
        return Collections.unmodifiableCollection(segments);
    }

    @Override
    public void update() {
        update(false);
    }

    @Override
    public void updateAll() {
        update(false);
    }

    private void update(boolean force) {
        Interpolator interpolator = null;
        Map<PropertyPart, InterpolationParameters> parameters = new HashMap<>();
        for (PathSegment segment : segments) {
            if (segment.getInterpolator() != interpolator) {
                interpolator = segment.getInterpolator();
                if (force || interpolator.isDirty()) {
                    parameters = interpolator.bake(parameters);
                }
            }
        }
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        PathSegment segment = getSegment(time);
        if (segment != null) {
            Interpolator interpolator = segment.getInterpolator();
            if (interpolator != null) {
                if (interpolator.getKeyframeProperties().contains(property)) {
                    return interpolator.getValue(property, time);
                }
            }
        }
        return Optional.absent();
    }

    @Override
    public Keyframe insert(long time) {
        Keyframe keyframe = new KeyframeImpl(time);
        insert(keyframe);
        return keyframe;
    }

    @Override
    public Keyframe getKeyframe(long time) {
        return keyframes.get(time);
    }

    @Override
    public void insert(Keyframe keyframe) {
        if (keyframes.containsKey(keyframe.getTime())) {
            throw new IllegalStateException("A keyframe at " + keyframe.getTime() + " already exists.");
        }
        keyframes.put(keyframe.getTime(), keyframe);

        if (segments.isEmpty()) {
            if (keyframes.size() >= 2) {
                Iterator<Keyframe> iter = keyframes.values().iterator();
                segments.add(new PathSegmentImpl(iter.next(), iter.next()));
            }
            return;
        }

        ListIterator<PathSegment> iter = segments.listIterator();
        PathSegment next = iter.next();
        if (keyframe.getTime() < next.getStartKeyframe().getTime()) {
            iter.previous();
            iter.add(new PathSegmentImpl(keyframe, next.getStartKeyframe(), next.getInterpolator()));
        }

        while (true) {
            if (next.getStartKeyframe().getTime() <= keyframe.getTime()
                    && next.getEndKeyframe().getTime() >= keyframe.getTime()) {
                iter.remove();
                next.setInterpolator(null);
                iter.add(new PathSegmentImpl(next.getStartKeyframe(), keyframe, next.getInterpolator()));
                iter.add(new PathSegmentImpl(keyframe, next.getEndKeyframe(), next.getInterpolator()));
                return;
            }
            if (iter.hasNext()) {
                next = iter.next();
            } else {
                iter.add(new PathSegmentImpl(next.getEndKeyframe(), keyframe, next.getInterpolator()));
                return;
            }
        }
    }

    @Override
    public void remove(Keyframe keyframe, boolean useFirstInterpolator) {
        if (keyframes.get(keyframe.getTime()) != keyframe) {
            throw new IllegalArgumentException("The keyframe " + keyframe + " is not part of this path.");
        }
        keyframes.remove(keyframe.getTime());

        if (segments.size() < 2) {
            for (PathSegment segment : segments) {
                segment.setInterpolator(null);
            }
            segments.clear();
            return;
        }

        ListIterator<PathSegment> iter = segments.listIterator();
        while (iter.hasNext()) {
            PathSegment next = iter.next();
            if (next.getEndKeyframe() == keyframe) {
                iter.remove();
                if (iter.hasNext()) {
                    PathSegment next2 = iter.next();
                    iter.remove();
                    iter.add(new PathSegmentImpl(next.getStartKeyframe(), next2.getEndKeyframe(),
                            (useFirstInterpolator ? next : next2).getInterpolator()));
                } else {
                    next.setInterpolator(null);
                }
                return;
            }
            if (next.getStartKeyframe() == keyframe) {
                next.setInterpolator(null);
                iter.remove();
                return;
            }
        }
        throw new AssertionError("No segment for keyframe found!");
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private PathSegment getSegment(long time) {
        for (PathSegment segment : segments) {
            if (segment.getStartKeyframe().getTime() <= time && segment.getEndKeyframe().getTime() >= time) {
                return segment;
            }
        }
        return null;
    }
}
