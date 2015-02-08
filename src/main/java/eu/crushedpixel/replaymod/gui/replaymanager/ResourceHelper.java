package eu.crushedpixel.replaymod.gui.replaymanager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import eu.crushedpixel.replaymod.reflection.MCPNames;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class ResourceHelper {

	private static BufferedImage defaultThumb;

	static {
		try {
			defaultThumb = ImageIO.read(MCPNames.class.getClassLoader().getResourceAsStream("default_thumb.jpg"));
		} catch(Exception e) {
			defaultThumb = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			e.printStackTrace();
		}
	}
	private static List<ResourceLocation> openResources = new ArrayList<ResourceLocation>();

	public static void registerResource(ResourceLocation loc) {
		openResources.add(loc);
	}

	public static void freeResource(ResourceLocation loc) {
		Minecraft.getMinecraft().getTextureManager().deleteTexture(loc);
		openResources.remove(loc);
	}

	public static void freeAllResources() {
		for(ResourceLocation loc : openResources) {
			Minecraft.getMinecraft().getTextureManager().deleteTexture(loc);
		}

		openResources = new ArrayList<ResourceLocation>();
	}

	public static BufferedImage getDefaultThumbnail() {
		return defaultThumb;
	}
}
