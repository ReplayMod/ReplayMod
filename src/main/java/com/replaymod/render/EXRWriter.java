//#if MC>=11400
package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.util.crash.CrashReport;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyexr.EXRChannelInfo;
import org.lwjgl.util.tinyexr.EXRHeader;
import org.lwjgl.util.tinyexr.EXRImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.tinyexr.TinyEXR.*;

public class EXRWriter implements FrameConsumer<BitmapFrame> {

    private final Path outputFolder;
    private final boolean keepAlpha;

    public EXRWriter(Path outputFolder, boolean keepAlpha) throws IOException {
        this.outputFolder = outputFolder;
        this.keepAlpha = keepAlpha;

        Files.createDirectories(outputFolder);
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame bgraFrame = channels.get(Channel.BRGA);
        BitmapFrame depthFrame = channels.get(Channel.DEPTH);

        Path path = outputFolder.resolve(bgraFrame.getFrameId() + ".exr");
        ReadableDimension size = bgraFrame.getSize();
        ByteBuffer bgra = bgraFrame.getByteBuffer();
        int width = size.getWidth();
        int height = size.getHeight();
        int numChannels = 4 + (depthFrame != null ? 1 : 0);

        stackPush();
        EXRHeader header = EXRHeader.mallocStack(); InitEXRHeader(header);
        EXRChannelInfo.Buffer channelInfos = EXRChannelInfo.mallocStack(numChannels);
        IntBuffer pixelTypes = stackMallocInt(numChannels);
        IntBuffer requestedPixelTypes = stackMallocInt(numChannels);
        EXRImage image = EXRImage.mallocStack(); InitEXRImage(image);
        PointerBuffer imagePointers = stackMallocPointer(numChannels);
        FloatBuffer images = memAllocFloat(width * height * numChannels);
        PointerBuffer err = stackMallocPointer(1);
        try {
            header.num_channels(numChannels);
            header.channels(channelInfos);
            header.pixel_types(pixelTypes);
            header.requested_pixel_types(requestedPixelTypes);

            // Some readers ignore this, so we use the most expected order
            memASCII("A", true, channelInfos.get(0).name());
            memASCII("B", true, channelInfos.get(1).name());
            memASCII("G", true, channelInfos.get(2).name());
            memASCII("R", true, channelInfos.get(3).name());
            for (int i = 0; i < numChannels; i++) {
                pixelTypes.put(i, TINYEXR_PIXELTYPE_FLOAT);
                requestedPixelTypes.put(i, TINYEXR_PIXELTYPE_HALF);
            }
            if (depthFrame != null) {
                memASCII("Z", true, channelInfos.get(4).name());
                requestedPixelTypes.put(4, TINYEXR_PIXELTYPE_FLOAT);
            }

            image.num_channels(numChannels);
            image.width(width);
            image.height(height);
            image.images(imagePointers);
            FloatBuffer[] bgrChannels = new FloatBuffer[4];
            FloatBuffer depthChannel = null;
            for (int i = 0; i < numChannels; i++) {
                FloatBuffer channel = images.slice();
                channel.position(width * height * i);
                imagePointers.put(i, channel.slice());
                if (i == 4) {
                    depthChannel = channel;
                } else {
                    bgrChannels[(i + 3) % 4] = channel;
                }
            }

            int alphaMask = keepAlpha ? 0 : 0xff;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    bgrChannels[0].put(((int) bgra.get() & 0xff) / 255f); // b
                    bgrChannels[1].put(((int) bgra.get() & 0xff) / 255f); // g
                    bgrChannels[2].put(((int) bgra.get() & 0xff) / 255f); // r
                    bgrChannels[3].put(((int) bgra.get() & 0xff | alphaMask) / 255f); // a
                }
            }
            if (depthFrame != null && depthChannel != null) {
                depthChannel.put(depthFrame.getByteBuffer().asFloatBuffer());
            }

            int ret = SaveEXRImageToFile(image, header, path.toString(), err);
            if (ret != TINYEXR_SUCCESS) {
                String message = err.getStringASCII(0);
                FreeEXRErrorMessage(err.getByteBuffer(0));
                throw new IOException(message);
            }
        } catch (Throwable t) {
            MCVer.getMinecraft().setCrashReport(CrashReport.create(t, "Exporting EXR frame"));
        } finally {
            memFree(images);
            stackPop();
            channels.values().forEach(it -> ByteBufferPool.release(it.getByteBuffer()));
        }
    }

    @Override
    public void close() {
    }
}
//#endif
