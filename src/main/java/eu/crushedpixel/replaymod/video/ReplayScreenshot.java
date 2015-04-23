package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.events.TickAndRenderListener;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ImageUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ReplayScreenshot {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static long last_finish = -1;

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
            GuiReplaySaving.replaySaving = true;

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

                        ReplayFileIO.addThumbToZip(replayFile, tempImage);

                        tempImage.delete();

						/*
                        File outputFile = File.createTempFile(replayFile.getName(), null);

						byte[] buf = new byte[1024];

						ZipInputStream zin = new ZipInputStream(new FileInputStream(replayFile));
						ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile));

						//copying all of the old Zip Entries to the new file, unless it's a thumb
						ZipEntry entry = zin.getNextEntry();
						while (entry != null) {
							String name = entry.getName();

							if(!name.contains("thumb")) {
								// Add ZIP entry to output stream.
								zout.putNextEntry(new ZipEntry(name));
								// Transfer bytes from the ZIP file to the output file
								int len;
								while ((len = zin.read(buf)) > 0) {
									zout.write(buf, 0, len);
								}
							} 

							entry = zin.getNextEntry();
						}

						FileInputStream fis = new FileInputStream(tempImage);

						zout.putNextEntry(new ZipEntry("thumb"));
						int len;
						//Add unique bytes to the end of the file
						zout.write(uniqueBytes);

						while ((len = fis.read(buf)) > 0) {
							zout.write(buf, 0, len);
						}


						fis.close();
						zin.close();

						zout.close();

						replayFile.delete();
						outputFile.renameTo(replayFile);

						tempImage.delete();
						*/

                        ReplayMod.chatMessageHandler.addChatMessage("Thumbnail has been successfully saved", ChatMessageType.INFORMATION);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        GuiReplaySaving.replaySaving = false;
                        locked = false;
                        TickAndRenderListener.finishScreenshot();
                    }
                }
            });

            ioThread.start();
        } catch(Exception exception) {
            exception.printStackTrace();
            mc.gameSettings.hideGUI = before;
            mc.currentScreen = beforeScreen;
            ReplayMod.replaySender.setReplaySpeed(beforeSpeed);
            exception.printStackTrace();
            ReplayMod.chatMessageHandler.addChatMessage("Thumbnail could not be saved", ChatMessageType.WARNING);
        }

        last_finish = System.currentTimeMillis();
    }


}
