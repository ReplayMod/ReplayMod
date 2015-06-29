package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.events.handlers.TickAndRenderListener;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ImageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ReplayScreenshot {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static boolean before;
    private static double beforeSpeed;
    private static GuiScreen beforeScreen;
    private static boolean locked = false;

    public static void prepareScreenshot() {
        before = mc.gameSettings.hideGUI;
        beforeSpeed = ReplayMod.replaySender.getReplaySpeed();
        beforeScreen = mc.currentScreen;
    }

    public static void saveScreenshot() {

        if(locked) return;
        locked = true;

        try {
            mc.gameSettings.hideGUI = true;
            mc.currentScreen = null;

            mc.entityRenderer.updateCameraAndRender(0);
            ReplayMod.replaySender.setReplaySpeed(0);

            final BufferedImage fbi = ScreenCapture.captureScreen();

            mc.gameSettings.hideGUI = before;
            mc.currentScreen = beforeScreen;
            ReplayMod.replaySender.setReplaySpeed(beforeSpeed);

            //The actual cropping and saving should be executed in a separate thread
            Thread ioThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        float aspect = 1280f / 720f;

                        Rectangle rect;
                        if((float) fbi.getWidth() / (float) fbi.getHeight() <= aspect) {
                            int h = Math.round(fbi.getWidth() / aspect);
                            int y = (fbi.getHeight() / 2) - (h / 2);
                            rect = new Rectangle(0, y, fbi.getWidth(), h);
                        } else {
                            int w = Math.round(fbi.getHeight() * aspect);
                            int x = (fbi.getWidth() / 2) - (w / 2);
                            rect = new Rectangle(x, 0, w, fbi.getHeight());
                        }

                        final BufferedImage nbi = ImageUtils.cropImage(fbi, rect);

                        File replayFile = ReplayHandler.getReplayFile();

                        File tempImage = File.createTempFile("thumb", null);

                        int h = 720;
                        int w = 1280;

                        BufferedImage img = ImageUtils.scaleImage(nbi, new Dimension(w, h));

                        ImageIO.write(img, "jpg", tempImage);

                        ReplayMod.replayFileAppender.registerModifiedFile(tempImage, "thumb", replayFile);

                        tempImage.deleteOnExit();

                        ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.savedthumb", ChatMessageType.INFORMATION);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        locked = false;
                        TickAndRenderListener.finishScreenshot();
                    }
                }
            }, "replaymod-screenshot-saver");

            ioThread.start();
        } catch(Exception exception) {
            exception.printStackTrace();
            mc.gameSettings.hideGUI = before;
            mc.currentScreen = beforeScreen;
            ReplayMod.replaySender.setReplaySpeed(beforeSpeed);
            exception.printStackTrace();
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.failedthumb", ChatMessageType.WARNING);
        }
    }
}
