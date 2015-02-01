package eu.crushedpixel.replaymod.registry;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.GuiIngameForge;
import eu.crushedpixel.replaymod.reflection.MCPNames;

public class ReplayGuiRegistry {

	private static Field renderHand;
	private static Minecraft mc = Minecraft.getMinecraft();
	
	public static boolean hidden = false;
	
	static {
		try {
			renderHand = EntityRenderer.class.getDeclaredField(MCPNames.field("field_175074_C"));
			renderHand.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void hide() {
		if(hidden) return;
		GuiIngameForge.renderExperiance = false;
		GuiIngameForge.renderArmor = false;
		GuiIngameForge.renderAir = false;
		GuiIngameForge.renderHealth = false;
		GuiIngameForge.renderHotbar = false;
		GuiIngameForge.renderFood = false;
		GuiIngameForge.renderBossHealth = false;
		GuiIngameForge.renderCrosshairs = false;
		GuiIngameForge.renderHelmet = false;
		GuiIngameForge.renderPortal = false;
		GuiIngameForge.renderHealthMount = false;
		GuiIngameForge.renderJumpBar = false;
		GuiIngameForge.renderObjective = false;
		
		try {
			renderHand.set(mc.entityRenderer, false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		hidden = true;
	}
	
	public static void show() {
		mc.gameSettings.hideGUI = false;
		
		GuiIngameForge.renderExperiance = true;
		GuiIngameForge.renderArmor = true;
		GuiIngameForge.renderAir = true;
		GuiIngameForge.renderHealth = true;
		GuiIngameForge.renderHotbar = true;
		GuiIngameForge.renderFood = true;
		GuiIngameForge.renderBossHealth = true;
		GuiIngameForge.renderCrosshairs = true;
		GuiIngameForge.renderHelmet = true;
		GuiIngameForge.renderPortal = true;
		GuiIngameForge.renderHealthMount = true;
		GuiIngameForge.renderJumpBar = true;
		GuiIngameForge.renderObjective = true;
		
		try {
			renderHand.set(mc.entityRenderer, true);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		hidden = false;
	}
}
