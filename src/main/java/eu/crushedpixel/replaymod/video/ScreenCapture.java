package eu.crushedpixel.replaymod.video;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.monte.screenrecorder.ScreenRecorder;

public class ScreenCapture {

	private static final Minecraft mc = Minecraft.getMinecraft();
	
	private static IntBuffer pixelBuffer;
	private static int[] pixelValues;
	
	public static BufferedImage captureScreen() {
		int width = mc.displayWidth;
		int height = mc.displayHeight;

		Framebuffer buffer = mc.getFramebuffer();
		
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
		
		
		return bufferedimage;
	}
}
