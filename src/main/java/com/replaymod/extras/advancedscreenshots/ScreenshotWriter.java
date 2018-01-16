package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.extras.ReplayModExtras;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import org.lwjgl.util.ReadableDimension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScreenshotWriter implements FrameConsumer<RGBFrame> {

    private final File outputFile;

    public ScreenshotWriter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void consume(RGBFrame frame) {
        // skip the first frame, in which not all chunks are properly loaded
        if (frame.getFrameId() == 0) return;

        try {
            final ReadableDimension frameSize = frame.getSize();

            BufferedImage img = new BufferedImage(frameSize.getWidth(), frameSize.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < frameSize.getHeight(); y++) {
                for (int x = 0; x < frameSize.getWidth(); x++) {
                    byte r = frame.getByteBuffer().get();
                    byte g = frame.getByteBuffer().get();
                    byte b = frame.getByteBuffer().get();

                    int color = ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, color);
                }
            }

            outputFile.getParentFile().mkdirs();
            ImageIO.write(img, "PNG", outputFile);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.makeCrashReport(e, "Exporting frame");
            Minecraft.getMinecraft().crashed(report);
        } catch (Throwable t) {
            CrashReport report = CrashReport.makeCrashReport(t, "Exporting frame");

            ReplayMod.instance.runLater(() -> Utils.error(ReplayModExtras.LOGGER,
                    ReplayModReplay.instance.getReplayHandler().getOverlay(),
                    report, null));
        }
    }

    @Override
    public void close() throws IOException {

    }
}
