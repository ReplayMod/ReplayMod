package com.replaymod.core.handler;

import com.replaymod.core.mixin.GuiMainMenuAccessor;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.realms.RealmsScreenProxy;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.resource.language.I18n;
//#else
//$$ import net.minecraft.client.gui.GuiButton;
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//#endif

/**
 * Moves certain buttons on the main menu upwards so we can inject our own.
 */
public class MainMenuHandler extends EventRegistrations {
    //#if MC>=11400
    { on(InitScreenCallback.EVENT, this::onInit); }
    public void onInit(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
    //$$     GuiScreen guiScreen = getGui(event);
    //$$     List<GuiButton> buttonList = getButtonList(event);
    //#endif
        if (guiScreen instanceof TitleScreen) {
            TitleScreen gui = (TitleScreen) guiScreen;
            int realmsOffset = 0;
            //#if MC>=11400
            final String BUTTON_REALMS = I18n.translate("menu.online");
            for (AbstractButtonWidget button : buttonList) {
            //#else
            //$$ for (GuiButton button : buttonList) {
            //#endif
                // Buttons that aren't in a rectangle directly above our space don't need moving
                if (button.x + width(button) < gui.width / 2 - 100
                        || button.x > gui.width / 2 + 100
                        || button.y > gui.height / 4 + 10 + 4 * 24) continue;
                // Move button up to make space for one rows of buttons
                // and then move back down by 10 to compensate for the space to the exit button that was already there
                int offset = -1 * 24 + 10;
                button.y += offset;

                //#if MC>=11300
                //#if MC>=11400
                if (BUTTON_REALMS.equals(button.getMessage())) {
                //#else
                //$$ if (button.id == 14) {
                //#endif
                    realmsOffset = offset;
                }
                //#endif
            }
            //#if MC>=11300
            GuiMainMenuAccessor guiA = (GuiMainMenuAccessor) gui;
            if (realmsOffset != 0 && guiA.getRealmsNotification() instanceof RealmsScreenProxy) {
                guiA.setRealmsNotification(new RealmsNotificationProxy((RealmsScreenProxy) guiA.getRealmsNotification(), realmsOffset));
            }
            //#endif
        }
    }

    //#if MC>=11300
    private static class RealmsNotificationProxy extends Screen {
        private final RealmsScreenProxy proxy;
        private final int offset;

        private RealmsNotificationProxy(RealmsScreenProxy proxy, int offset) {
            //#if MC>=11400
            super(null);
            //#endif
            this.proxy = proxy;
            this.offset = offset;
        }

        @Override
        public void init(MinecraftClient mc, int width, int height) {
            proxy.init(mc, width, height);
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
        public void removed() {
            proxy.removed();
        }
    }
    //#endif
}
