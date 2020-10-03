package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.extras.ReplayModExtras;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.versions.Image;
import net.minecraft.util.crash.CrashReport;

import java.io.File;
import java.io.IOException;

public class ScreenshotWriter implements FrameConsumer<BitmapFrame> {

    private final File outputFile;

    public ScreenshotWriter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void consume(BitmapFrame frame) {
        // skip the first frame, in which not all chunks are properly loaded
        if (frame.getFrameId() == 0) return;

        final ReadableDimension frameSize = frame.getSize();
        try (Image img = new Image(frameSize.getWidth(), frameSize.getHeight())) {
            for (int y = 0; y < frameSize.getHeight(); y++) {
                for (int x = 0; x < frameSize.getWidth(); x++) {
                    byte b = frame.getByteBuffer().get();
                    byte g = frame.getByteBuffer().get();
                    byte r = frame.getByteBuffer().get();
                    byte a = frame.getByteBuffer().get();

                    img.setRGBA(x, y, r, g, b, 0xff);
                }
            }

            outputFile.getParentFile().mkdirs();
            img.writePNG(outputFile);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.create(e, "Exporting frame");
            MCVer.getMinecraft().setCrashReport(report);
        } catch (Throwable t) {
            CrashReport report = CrashReport.create(t, "Exporting frame");

            ReplayMod.instance.runLater(() -> Utils.error(ReplayModExtras.LOGGER,
                    ReplayModReplay.instance.getReplayHandler().getOverlay(),
                    report, null));
        }
    }

    @Override
    public void close() throws IOException {

    }
}
