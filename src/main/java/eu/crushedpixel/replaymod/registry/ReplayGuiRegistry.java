package eu.crushedpixel.replaymod.registry;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;

public class ReplayGuiRegistry {

    public static boolean hidden = false;
    private static Minecraft mc = Minecraft.getMinecraft();

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

        hidden = false;
    }
}
