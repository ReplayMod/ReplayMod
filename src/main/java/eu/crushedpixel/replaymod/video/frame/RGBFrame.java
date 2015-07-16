package eu.crushedpixel.replaymod.video.frame;

import eu.crushedpixel.replaymod.video.rendering.Frame;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.lwjgl.util.ReadableDimension;

import java.nio.ByteBuffer;

public class RGBFrame implements Frame {
    @Getter
    private final int frameId;

    @Getter
    private final ReadableDimension size;

    @Getter
    private final ByteBuffer byteBuffer;

    public RGBFrame(int frameId, ReadableDimension size, ByteBuffer byteBuffer) {
        Validate.isTrue(size.getWidth() * size.getHeight() * 3 == byteBuffer.remaining(),
                "Buffer size is %d (cap: %d) but should be %d",
                byteBuffer.remaining(), byteBuffer.capacity(), size.getWidth() * size.getHeight() * 4);
        this.frameId = frameId;
        this.size = size;
        this.byteBuffer = byteBuffer;
    }
}
