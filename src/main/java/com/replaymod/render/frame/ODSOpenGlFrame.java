package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public class ODSOpenGlFrame implements Frame {
    private final CubicOpenGlFrame left, right;

    public ODSOpenGlFrame(CubicOpenGlFrame left, CubicOpenGlFrame right) {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
        this.left = left;
        this.right = right;
    }

    @Override
    public int getFrameId() {
        return left.getFrameId();
    }

    public CubicOpenGlFrame getLeft() {
        return this.left;
    }

    public CubicOpenGlFrame getRight() {
        return this.right;
    }
}
