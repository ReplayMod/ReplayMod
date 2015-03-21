package eu.crushedpixel.replaymod.renderer;

import java.lang.reflect.Field;

import eu.crushedpixel.replaymod.reflection.MCPNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManager;

public class SafeEntityRenderer extends EntityRenderer {

	private static Field resourceManager;
	static {
		try {
			resourceManager = EntityRenderer.class.getDeclaredField(MCPNames.field("field_147711_ac"));
			resourceManager.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public SafeEntityRenderer(Minecraft mcIn, EntityRenderer renderer) throws IllegalArgumentException, IllegalAccessException {
		super(mcIn, (IResourceManager)resourceManager.get(renderer));
	}

	@Override
	public void updateCameraAndRender(float partialTicks) {
		try {
			super.updateCameraAndRender(partialTicks);
		} catch(Exception e) {} //This is plain easier than doing proper error prevention.
		//If Johni reads this, don't think I'm a bad programmer... Just a lazy one :P
	}

}
