package com.replaymod.render.frame;

import com.replaymod.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class OpenGlFrame implements Frame {
    @Getter
    private final int frameId;

    @Getter
    private final ReadableDimension size;

    @Getter
    private final ByteBuffer byteBuffer;
}
