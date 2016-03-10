package com.replaymod.pathing.impl;

import com.replaymod.pathing.interpolation.Interpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.PathSegment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PathSegmentImpl implements PathSegment {
    private final Keyframe startKeyframe;
    private final Keyframe endKeyframe;
    private Interpolator interpolator;

    public PathSegmentImpl(Keyframe startKeyframe, Keyframe endKeyframe, Interpolator interpolator) {
        this.startKeyframe = startKeyframe;
        this.endKeyframe = endKeyframe;
        setInterpolator(interpolator);
    }

    @Override
    public void setInterpolator(Interpolator interpolator) {
        if (this.interpolator != null) {
            this.interpolator.removeSegment(this);
        }
        this.interpolator = interpolator;
        if (this.interpolator != null) {
            this.interpolator.addSegment(this);
        }
    }
}
