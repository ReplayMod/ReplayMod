package com.replaymod.render.frame;

import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

public class OpenGlTextureFrame extends OpenGlFrame {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private final int textureTarget;
    private final int textureId;

    public OpenGlTextureFrame(int frameId, ReadableDimension size, int textureTarget, int textureId) {
        super(frameId, size, 4, EMPTY_BUFFER);
        this.textureTarget = textureTarget;
        this.textureId = textureId;
    }

    public int getTextureTarget() {
        return textureTarget;
    }

    public int getTextureId() {
        return textureId;
    }
}
