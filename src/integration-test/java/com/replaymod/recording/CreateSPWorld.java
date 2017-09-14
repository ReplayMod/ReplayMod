package com.replaymod.recording;

import com.replaymod.core.AbstractTask;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

public class CreateSPWorld extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiMainMenu.class, mainMenu -> {
            click("Singleplayer");
            expectGui(GuiSelectWorld.class, selectWorld -> {
                click("Create New World");
                expectGui(GuiCreateWorld.class, createWorld -> {
                    click("Create New World");
                    MinecraftForge.EVENT_BUS.register(new EventHandler());
                });
            });
        });
    }

    public class EventHandler {
        @SubscribeEvent
        public void onRenderIngame(RenderGameOverlayEvent.Pre event) {
            MinecraftForge.EVENT_BUS.unregister(this);
            runLater(() -> future.set(null));
        }
    }
}
