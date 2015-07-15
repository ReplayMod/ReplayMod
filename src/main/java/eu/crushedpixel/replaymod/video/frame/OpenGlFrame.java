package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.rendering.Frame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.lwjgl.util.ReadableDimension;

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
