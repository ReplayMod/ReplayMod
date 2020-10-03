//#if MC>=11400
package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.frame.EXRFrame;
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

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.tinyexr.TinyEXR.*;

public class EXRWriter implements FrameConsumer<EXRFrame> {

    private final Path outputFolder;

    public EXRWriter(Path outputFolder) throws IOException {
        this.outputFolder = outputFolder;

        Files.createDirectories(outputFolder);
    }

    @Override
    public void consume(EXRFrame frame) {
        Path path = outputFolder.resolve(frame.getFrameId() + ".exr");
        ReadableDimension size = frame.getSize();
        ByteBuffer bgra = frame.getBgraBuffer();
        int width = size.getWidth();
        int height = size.getHeight();
        int numChannels = 3;

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
            memASCII("B", true, channelInfos.get(0).name());
            memASCII("G", true, channelInfos.get(1).name());
            memASCII("R", true, channelInfos.get(2).name());
            for (int i = 0; i < numChannels; i++) {
                pixelTypes.put(i, TINYEXR_PIXELTYPE_FLOAT);
                requestedPixelTypes.put(i, TINYEXR_PIXELTYPE_HALF);
            }

            image.num_channels(numChannels);
            image.width(width);
            image.height(height);
            image.images(imagePointers);
            FloatBuffer[] bgrChannels = new FloatBuffer[3];
            for (int i = 0; i < numChannels; i++) {
                FloatBuffer channel = images.slice();
                channel.position(width * height * i);
                imagePointers.put(i, channel.slice());
                bgrChannels[i] = channel;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (FloatBuffer channel : bgrChannels) {
                        channel.put(((int) bgra.get() & 0xff) / 255f);
                    }
                    bgra.get(); // alpha
                }
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
            ByteBufferPool.release(bgra);
        }
    }

    @Override
    public void close() {
    }
}
//#endif
