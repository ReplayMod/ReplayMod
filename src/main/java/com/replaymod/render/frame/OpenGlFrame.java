package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

public class OpenGlFrame implements Frame {
    private final int frameId;

    private final ReadableDimension size;

    private final ByteBuffer byteBuffer;

    public OpenGlFrame(int frameId, ReadableDimension size, ByteBuffer byteBuffer) {
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
