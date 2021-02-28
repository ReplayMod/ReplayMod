package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.Util.IOConsumer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.versions.Image;
import net.minecraft.util.crash.CrashReport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PNGWriter implements FrameConsumer<BitmapFrame> {

    private final Path outputFolder;

    public PNGWriter(Path outputFolder) throws IOException {
        this.outputFolder = outputFolder;

        Files.createDirectories(outputFolder);
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame bgraFrame = channels.get(Channel.BRGA);
        BitmapFrame depthFrame = channels.get(Channel.DEPTH);
        try {
            if (bgraFrame != null) {
                withImage(bgraFrame, image ->
                        image.writePNG(outputFolder.resolve(bgraFrame.getFrameId() + ".png").toFile()));
            }
            if (depthFrame != null) {
                withImage(depthFrame, image ->
                        image.writePNG(outputFolder.resolve(depthFrame.getFrameId() + ".depth.png").toFile()));
            }
        } catch (Throwable t) {
            MCVer.getMinecraft().setCrashReport(CrashReport.create(t, "Exporting EXR frame"));
        } finally {
            channels.values().forEach(it -> ByteBufferPool.release(it.getByteBuffer()));
        }
    }

    private void withImage(BitmapFrame frame, IOConsumer<Image> consumer) throws IOException {
        ByteBuffer buffer = frame.getByteBuffer();
        ReadableDimension size = frame.getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        try (Image image = new Image(width, height)) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte b = buffer.get();
                    byte g = buffer.get();
                    byte r = buffer.get();
                    byte a = buffer.get();
                    image.setRGBA(x, y, r, g, b, a);
                }
            }
            consumer.accept(image);
        }
    }

    @Override
    public void close() {
    }
}
