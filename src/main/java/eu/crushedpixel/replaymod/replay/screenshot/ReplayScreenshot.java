package eu.crushedpixel.replaymod.replay.screenshot;

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
	private static IntBuffer pixelBuffer;
	private static int[] pixelValues;

	private static final byte[] uniqueBytes = new byte[]{0,1,1,2,3,5,8};

	private static long last_finish = -1;

	private static boolean before;
	private static double beforeSpeed;
	private static GuiScreen beforeScreen;

	public static void prepareScreenshot() {
		before = mc.gameSettings.hideGUI;
		beforeSpeed = ReplayHandler.getSpeed();
		beforeScreen = mc.currentScreen;
	}

	public static void saveScreenshot(Framebuffer buffer) {

		try {
			GuiReplaySaving.replaySaving = true;

			mc.gameSettings.hideGUI = true;
			mc.currentScreen = null;

			mc.entityRenderer.updateCameraAndRender(0);
			ReplayHandler.setSpeed(0);

			int width = mc.displayWidth;
			int height = mc.displayHeight;

			if (OpenGlHelper.isFramebufferEnabled())
			{
				width = buffer.framebufferTextureWidth;
				height = buffer.framebufferTextureHeight;
			}

			int k = width * height;

			if (pixelBuffer == null || pixelBuffer.capacity() < k)
			{
				pixelBuffer = BufferUtils.createIntBuffer(k);
				pixelValues = new int[k];
			}

			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
			pixelBuffer.clear();

			if (OpenGlHelper.isFramebufferEnabled()) {
				GlStateManager.bindTexture(buffer.framebufferTexture);
				GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			else {
				GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}

			pixelBuffer.get(pixelValues);
			TextureUtil.processPixelValues(pixelValues, width, height);
			BufferedImage bufferedimage = null;


			if (OpenGlHelper.isFramebufferEnabled())
			{
				bufferedimage = new BufferedImage(buffer.framebufferWidth, buffer.framebufferHeight, 1);
				int l = buffer.framebufferTextureHeight - buffer.framebufferHeight;

				for (int i1 = l; i1 < buffer.framebufferTextureHeight; ++i1)
				{
					for (int j1 = 0; j1 < buffer.framebufferWidth; ++j1)
					{
						bufferedimage.setRGB(j1, i1 - l, pixelValues[i1 * buffer.framebufferTextureWidth + j1]);
					}
				}
			}
			else {	 
				bufferedimage = new BufferedImage(width, height, 1);
				bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);
			}

			final BufferedImage fbi = bufferedimage;

			mc.gameSettings.hideGUI = before;
			mc.currentScreen = beforeScreen;
			ReplayHandler.setSpeed(beforeSpeed);

			//The actual cropping and saving should be executed in a separate thread
			Thread ioThread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {


						float aspect = 1280f/720f;

						Rectangle rect;
						if((float)fbi.getWidth()/(float)fbi.getHeight() <= aspect) {
							int h = Math.round(fbi.getWidth()/aspect);
							int y = (fbi.getHeight()/2) - (h/2);
							System.out.println(h+" | "+y);
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
						//int w = width*(h/height);

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

						ChatMessageRequests.addChatMessage("Thumbnail has been successfully saved", ChatMessageType.INFORMATION);
					} catch(Exception e) {}
					finally {
						GuiReplaySaving.replaySaving = false;
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
