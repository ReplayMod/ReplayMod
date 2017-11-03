package com.replaymod.core.handler;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Moves certain buttons on the main menu upwards so we can inject our own.
 */
public class MainMenuHandler {
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            GuiMainMenu gui = (GuiMainMenu) event.getGui();
            int realmsOffset = 0;
            for (GuiButton button : event.getButtonList()) {
                // Buttons that aren't in a rectangle directly above our space don't need moving
                if (button.x + button.width < event.getGui().width / 2 - 100
                        || button.x > event.getGui().width / 2 + 100
                        || button.y > event.getGui().height / 4 + 10 + 4 * 24) continue;
                // Move button up to make space for two rows of buttons
                // and then move back down by 10 to compensate for the space to the exit button that was already there
                int offset = -2 * 24 + 10;
                button.y += offset;
                if (button == gui.realmsButton) {
                    realmsOffset = offset;
                }
            }
            if (realmsOffset != 0 && gui.realmsNotification instanceof GuiScreenRealmsProxy) {
                gui.realmsNotification = new RealmsNotificationProxy((GuiScreenRealmsProxy) gui.realmsNotification, realmsOffset);
            }
        }
    }

    private static class RealmsNotificationProxy extends GuiScreen {
        private final GuiScreenRealmsProxy proxy;
        private final int offset;

        private RealmsNotificationProxy(GuiScreenRealmsProxy proxy, int offset) {
            this.proxy = proxy;
            this.offset = offset;
        }

        @Override
        public void setGuiSize(int w, int h) {
            proxy.setGuiSize(w, h);
        }

        @Override
        public void initGui() {
            proxy.initGui();
        }

        @Override
        public void updateScreen() {
            proxy.updateScreen();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            GL11.glTranslated(0, offset, 0);
            proxy.drawScreen(mouseX, mouseY - offset, partialTicks);
            GL11.glTranslated(0, -offset, 0);
        }

        @Override
        public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            proxy.mouseClicked(mouseX, mouseY - offset, mouseButton);
        }

        @Override
        public void onGuiClosed() {
            proxy.onGuiClosed();
        }
    }
}
