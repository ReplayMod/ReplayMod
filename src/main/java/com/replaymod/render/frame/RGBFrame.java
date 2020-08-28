package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

public class RGBFrame implements Frame {
    private final int frameId;

    private final ReadableDimension size;

    private final ByteBuffer byteBuffer;

    public RGBFrame(int frameId, ReadableDimension size, ByteBuffer byteBuffer) {
        Validate.isTrue(size.getWidth() * size.getHeight() * 4 == byteBuffer.remaining(),
                "Buffer size is %d (cap: %d) but should be %d",
                byteBuffer.remaining(), byteBuffer.capacity(), size.getWidth() * size.getHeight() * 4);
        this.frameId = frameId;
        this.size = size;
        this.byteBuffer = byteBuffer;
    }

    public int getFrameId() {
        return this.frameId;
    }

    public ReadableDimension getSize() {
        return this.size;
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }
}
