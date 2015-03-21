package eu.crushedpixel.replaymod.video;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ImageUtils;

public class ReplayScreenshot {

	private static Minecraft mc = Minecraft.getMinecraft();

	private static final byte[] uniqueBytes = new byte[]{0,1,1,2,3,5,8};

	private static long last_finish = -1;

	private static boolean before;
	private static double beforeSpeed;
	private static GuiScreen beforeScreen;

	public static void prepareScreenshot() {
		System.out.println("thumbnail preparing");
		before = mc.gameSettings.hideGUI;
		beforeSpeed = ReplayHandler.getSpeed();
		beforeScreen = mc.currentScreen;
	}
	
	private static boolean locked = false;

	public static void saveScreenshot(Framebuffer buffer) {

		if(locked) return;
		locked = true;
		
		System.out.println("thumbnail started");
		try {
			GuiReplaySaving.replaySaving = true;

			mc.gameSettings.hideGUI = true;
			mc.currentScreen = null;

			mc.entityRenderer.updateCameraAndRender(0);
			ReplayHandler.setSpeed(0);

			final BufferedImage fbi = ScreenCapture.captureScreen();

			mc.gameSettings.hideGUI = before;
			mc.currentScreen = beforeScreen;
			ReplayHandler.setSpeed(beforeSpeed);

			//The actual cropping and saving should be executed in a separate thread
			Thread ioThread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						System.out.println("thumbnail saving started");
						float aspect = 1280f/720f;

						Rectangle rect;
						if((float)fbi.getWidth()/(float)fbi.getHeight() <= aspect) {
							int h = Math.round(fbi.getWidth()/aspect);
							int y = (fbi.getHeight()/2) - (h/2);
							rect = new Rectangle(0, y, fbi.getWidth(), h);
						} else {
							int w = Math.round(fbi.getHeight()*aspect);
							int x = (fbi.getWidth()/2) - (w/2);
							rect = new Rectangle(x, 0, w, fbi.getHeight());
						}

						final BufferedImage nbi = ImageUtils.cropImage(fbi, rect);

						File replayFile = ReplayHandler.getReplayFile();

						File folder = new File("./replay_recordings/");
						folder.mkdirs();

						File temp = File.createTempFile("thumb", null);

						int h = 720;
						int w = 1280;

						BufferedImage img = ImageUtils.scaleImage(nbi, new Dimension(w, h));

						ImageIO.write(img, "jpg", temp);

						File tempFile = File.createTempFile(replayFile.getName(), null);
						tempFile.delete(); tempFile.deleteOnExit();

						replayFile.renameTo(tempFile);

						replayFile.delete();
						replayFile.createNewFile();

						byte[] buf = new byte[1024];

						ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
						ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(replayFile));

						FileInputStream fis = new FileInputStream(temp);

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

						zout.putNextEntry(new ZipEntry("thumb"));
						int len;
						//Add unique bytes to the end of the file
						zout.write(uniqueBytes);

						while ((len = fis.read(buf)) > 0) {
							zout.write(buf, 0, len);
						}

						// Close the streams
						fis.close();
						zin.close();
						// Compress the files
						// Complete the ZIP file
						zout.close();
						tempFile.delete();
						temp.delete();
						System.out.println("thumbnail saving finished");

						ChatMessageRequests.addChatMessage("Thumbnail has been successfully saved", ChatMessageType.INFORMATION);
					} catch(Exception e) {}
					finally {
						GuiReplaySaving.replaySaving = false;
						locked = false;
					}
				}
			});

			ioThread.start();
		}
		catch (Exception exception) {
			mc.gameSettings.hideGUI = before;
			mc.currentScreen = beforeScreen;
			ReplayHandler.setSpeed(beforeSpeed);
			exception.printStackTrace();
			ChatMessageRequests.addChatMessage("Thumbnail could not be saved", ChatMessageType.WARNING);
		}

		last_finish = System.currentTimeMillis();
	}


}
