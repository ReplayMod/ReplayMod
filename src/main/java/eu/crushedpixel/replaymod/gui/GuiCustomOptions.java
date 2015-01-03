package eu.crushedpixel.replaymod.gui;

import java.io.IOException;
import java.lang.reflect.Field;

import com.mojang.realmsclient.util.Pair;

import eu.crushedpixel.replaymod.reflection.MCPNames;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.settings.GameSettings;

public class GuiCustomOptions extends GuiOptions {

	public static GuiScreen getGuiScreen(GuiOptions go) {
		try {
			Class<GuiOptions> clazz = GuiOptions.class;
			Field guiScreen = clazz.getDeclaredField(MCPNames.field(MCPNames.field("field_146441_g")));
			guiScreen.setAccessible(true);
			Object obj = guiScreen.get(go);
			return (GuiScreen)obj;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static GameSettings getGameSettings(GuiOptions go) {
		try {
			Class<GuiOptions> clazz = GuiOptions.class;
			Field gameSettings = clazz.getDeclaredField(MCPNames.field(MCPNames.field("field_146443_h")));
			gameSettings.setAccessible(true);
			Object obj = gameSettings.get(go);
			return (GameSettings)obj;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public GuiCustomOptions(GuiScreen guiScreen, GameSettings gameSettings) {
		super(guiScreen, gameSettings);
	}

	@Override
	public void initGui() {
		super.initGui();
		buttonList.add(new GuiButton(9001, this.width / 2 - 155, this.height / 6 + 48 - 6 - 24, 310, 20, "Replay Mod Settings..."));
	}

	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.enabled && button.id == 9001) { //Replay Mod Settings...
			this.mc.displayGuiScreen(new GuiReplaySettings(this));
		}
	}

}
