package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

public class CubicOpenGlFrame implements Frame {
    @Getter
    private final OpenGlFrame left, right, front, back, top, bottom;

    public CubicOpenGlFrame(OpenGlFrame left, OpenGlFrame right, OpenGlFrame front, OpenGlFrame back, OpenGlFrame top, OpenGlFrame bottom) {
        Validate.isTrue(left.getFrameId() == right.getFrameId()
                && right.getFrameId() == front.getFrameId()
                && front.getFrameId() == back.getFrameId()
                && back.getFrameId() == top.getFrameId()
                && top.getFrameId() == bottom.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining()
                && right.getByteBuffer().remaining() == front.getByteBuffer().remaining()
                && front.getByteBuffer().remaining() == back.getByteBuffer().remaining()
                && back.getByteBuffer().remaining() == top.getByteBuffer().remaining()
                && top.getByteBuffer().remaining() == bottom.getByteBuffer().remaining(), "Buffer size does not match.");
        this.left = left;
        this.right = right;
        this.front = front;
        this.back = back;
        this.top = top;
        this.bottom = bottom;
    }

    @Override
    public int getFrameId() {
        return left.getFrameId();
    }
}
