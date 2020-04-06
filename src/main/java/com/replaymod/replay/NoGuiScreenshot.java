package com.replaymod.replay;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import de.johni0702.minecraft.gui.versions.Image;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.util.ScreenshotUtils;

//#if MC>=11500
import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11400
import static com.replaymod.core.versions.MCVer.getWindow;
//#endif

//#if MC<11400
//$$ import com.google.common.io.Files;
//$$ import org.apache.commons.io.FileUtils;
//$$ import java.io.File;
//#endif

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class NoGuiScreenshot {
    private final Image image;
    private final int width;
    private final int height;

    public static ListenableFuture<NoGuiScreenshot> take(final MinecraftClient mc, final int width, final int height) {
        final SettableFuture<NoGuiScreenshot> future = SettableFuture.create();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (future.isCancelled()) {
                    return;
                }

                //#if MC>=11400
                int frameWidth = getWindow(mc).getFramebufferWidth(), frameHeight = getWindow(mc).getFramebufferHeight();
                //#else
                //$$ int frameWidth = mc.displayWidth, frameHeight = mc.displayHeight;
                //#endif

                final boolean guiHidden = mc.options.hudHidden;
                try {
                    mc.options.hudHidden = true;

                    // Render frame without GUI
                    GlStateManager.pushMatrix();
                    GlStateManager.clear(
                            16640
                            //#if MC>=11400
                            , true
                            //#endif
                    );
                    mc.getFramebuffer().beginWrite(true);
                    GlStateManager.enableTexture();

                    //#if MC>=11500
                    mc.gameRenderer.renderWorld(MCVer.getRenderPartialTicks(), System.nanoTime(), new MatrixStack());
                    //#else
                    //#if MC>=11400
                    //$$ mc.gameRenderer.renderWorld(MCVer.getRenderPartialTicks(), System.nanoTime());
                    //#else
                    //#if MC>=10809
                    //$$ mc.entityRenderer.updateCameraAndRender(MCVer.getRenderPartialTicks(), System.nanoTime());
                    //#else
                    //$$ mc.entityRenderer.updateCameraAndRender(MCVer.getRenderPartialTicks());
                    //#endif
                    //#endif
                    //#endif

                    mc.getFramebuffer().endWrite();
                    GlStateManager.popMatrix();
                    GlStateManager.pushMatrix();
                    mc.getFramebuffer().draw(frameWidth, frameHeight);
                    GlStateManager.popMatrix();
                } catch (Throwable t) {
                    future.setException(t);
                    return;
                } finally {
                    // Reset GUI settings
                    mc.options.hudHidden = guiHidden;
                }

                // The frame without GUI has been rendered
                // Read it, create the screenshot and finish the future
                try {
                    //#if MC>=11400
                    Image image = new Image(ScreenshotUtils.takeScreenshot(frameWidth, frameHeight, mc.getFramebuffer()));
                    //#else
                    //$$ // We're using Minecraft's ScreenShotHelper even though it writes the screenshot to
                    //$$ // disk for better maintainability
                    //$$ File tmpFolder = Files.createTempDir();
                    //$$ Image image;
                    //$$ try {
                    //$$     ScreenShotHelper.saveScreenshot(tmpFolder, "tmp", frameWidth, frameHeight, mc.getFramebuffer());
                    //$$     File screenshotFile = new File(tmpFolder, "screenshots/tmp");
                    //$$     image = Image.read(screenshotFile.toPath());
                    //$$ } finally {
                    //$$     FileUtils.deleteQuietly(tmpFolder);
                    //$$ }
                    //#endif
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    // Scale & crop
                    float scaleFactor = Math.max((float) width / imageWidth, (float) height / imageHeight);
                    int croppedWidth = Math.min(Math.max(0, (int) (width / scaleFactor)), imageWidth);
                    int croppedHeight = Math.min(Math.max(0, (int) (height / scaleFactor)), imageHeight);
                    int offsetX = (imageWidth - croppedWidth) / 2;
                    int offsetY = (imageHeight - croppedHeight) / 2;
                    image = image.scaledSubRect(offsetX, offsetY, croppedWidth, croppedHeight, width, height);

                    // Finish
                    future.set(new NoGuiScreenshot(image, width, height));
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        };

        // Make sure we are not somewhere in the middle of the rendering process but always at the beginning
        // of the game loop. We cannot use the addScheduledTask method as it'll run the task if called
        // from the minecraft thread which is exactly what we want to avoid.
        ReplayMod.instance.runLater(runnable);
        return future;
    }
}
