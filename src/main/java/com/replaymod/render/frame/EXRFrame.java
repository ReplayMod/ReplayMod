package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

public class EXRFrame implements Frame {
    private final int frameId;
    private final ReadableDimension size;
    private final ByteBuffer bgraBuffer;

    public EXRFrame(int frameId, ReadableDimension size, ByteBuffer bgraBuffer) {
        Validate.isTrue(size.getWidth() * size.getHeight() * 4 == bgraBuffer.remaining(),
                "BGRA buffer size is %d (cap: %d) but should be %d",
                bgraBuffer.remaining(), bgraBuffer.capacity(), size.getWidth() * size.getHeight() * 4);
        this.frameId = frameId;
        this.size = size;
        this.bgraBuffer = bgraBuffer;
    }

    public int getFrameId() {
        return this.frameId;
    }

    public ReadableDimension getSize() {
        return this.size;
    }

    public ByteBuffer getBgraBuffer() {
        return this.bgraBuffer;
    }
}
