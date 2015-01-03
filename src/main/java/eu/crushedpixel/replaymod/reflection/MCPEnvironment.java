package eu.crushedpixel.replaymod.reflection;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiMainMenu;

public class MCPEnvironment {

	boolean eclipse = true;

	public MCPEnvironment() {
		eclipse = true;
		Class<? extends GuiMainMenu> clazz = GuiMainMenu.class;
		try {
			Field viewportTexture = clazz.getDeclaredField("viewportTexture");
		} catch(Exception e) {
			eclipse = false;
		}
	}

	public boolean isMCPEnvironment() {
		return eclipse;
	}
}
