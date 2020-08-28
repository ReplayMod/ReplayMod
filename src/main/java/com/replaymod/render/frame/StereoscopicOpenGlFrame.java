package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public class StereoscopicOpenGlFrame implements Frame {
    private final OpenGlFrame left, right;

    public StereoscopicOpenGlFrame(OpenGlFrame left, OpenGlFrame right) {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining(), "Buffer size does not match.");
        this.left = left;
        this.right = right;
    }

    @Override
    public int getFrameId() {
        return left.getFrameId();
    }

    public OpenGlFrame getLeft() {
        return this.left;
    }

    public OpenGlFrame getRight() {
        return this.right;
    }
}
