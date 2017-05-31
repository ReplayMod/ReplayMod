package com.replaymod.recording;

import com.replaymod.core.AbstractTask;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CreateSPWorld extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiMainMenu.class, mainMenu -> {
            click("Singleplayer");
            expectGui(GuiWorldSelection.class, selectWorld -> {
                click("Create New World");
                expectGui(GuiCreateWorld.class, createWorld -> {
                    click("Create New World");
                    class EventHandler {
                        @SubscribeEvent
                        public void onRenderIngame(RenderGameOverlayEvent.Pre event) {
                            MinecraftForge.EVENT_BUS.unregister(this);
                            runLater(() -> future.set(null));
                        }
                    }
                    MinecraftForge.EVENT_BUS.register(new EventHandler());
                });
            });
        });
    }
}
