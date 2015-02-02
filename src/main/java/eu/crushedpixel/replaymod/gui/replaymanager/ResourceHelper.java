package eu.crushedpixel.replaymod.gui.replaymanager;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class ResourceHelper {

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
}
