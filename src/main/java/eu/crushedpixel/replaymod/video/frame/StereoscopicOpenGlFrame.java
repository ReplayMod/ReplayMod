package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.rendering.Frame;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

public class StereoscopicOpenGlFrame implements Frame {
    @Getter
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
}
