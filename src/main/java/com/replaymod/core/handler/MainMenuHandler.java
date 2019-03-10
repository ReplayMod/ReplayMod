package com.replaymod.core.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import static com.replaymod.core.versions.MCVer.*;

/**
 * Moves certain buttons on the main menu upwards so we can inject our own.
 */
public class MainMenuHandler {
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen guiScreen = getGui(event);
        if (guiScreen instanceof GuiMainMenu) {
            GuiMainMenu gui = (GuiMainMenu) guiScreen;
            int realmsOffset = 0;
            for (GuiButton button : getButtonList(event)) {
                // Buttons that aren't in a rectangle directly above our space don't need moving
                if (x(button) + button.width < gui.width / 2 - 100
                        || x(button) > gui.width / 2 + 100
                        || y(button) > gui.height / 4 + 10 + 4 * 24) continue;
                // Move button up to make space for two rows of buttons
                // and then move back down by 10 to compensate for the space to the exit button that was already there
                int offset = -2 * 24 + 10;
                y(button, y(button) + offset);

                //#if MC>=11300
                if (button.id == 14) {
                    realmsOffset = offset;
                }
                //#endif
            }
            //#if MC>=11300
            if (realmsOffset != 0 && gui.realmsNotification instanceof GuiScreenRealmsProxy) {
                gui.realmsNotification = new RealmsNotificationProxy((GuiScreenRealmsProxy) gui.realmsNotification, realmsOffset);
            }
            //#endif
        }
    }

    //#if MC>=11300
    private static class RealmsNotificationProxy extends GuiScreen {
        private final GuiScreenRealmsProxy proxy;
        private final int offset;

        private RealmsNotificationProxy(GuiScreenRealmsProxy proxy, int offset) {
            this.proxy = proxy;
            this.offset = offset;
        }

        @Override
        public void setWorldAndResolution(Minecraft mc, int width, int height) {
            proxy.setWorldAndResolution(mc, width, height);
        }

        @Override
        public void tick() {
            proxy.tick();
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            GL11.glTranslated(0, offset, 0);
            proxy.render(mouseX, mouseY - offset, partialTicks);
            GL11.glTranslated(0, -offset, 0);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            return proxy.mouseClicked(mouseX, mouseY - offset, mouseButton);
        }

        @Override
        public void onGuiClosed() {
            proxy.onGuiClosed();
        }
    }
    //#endif
}
