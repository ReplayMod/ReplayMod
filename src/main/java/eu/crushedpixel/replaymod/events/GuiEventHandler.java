package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.gui.GuiCustomMainMenu;
import eu.crushedpixel.replaymod.gui.GuiCustomOptions;
import eu.crushedpixel.replaymod.gui.GuiExitReplay;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiEventHandler {

	@SubscribeEvent
	public void onGui(GuiOpenEvent event) {
		if(event.gui instanceof GuiMainMenu) {
			event.gui = new GuiCustomMainMenu();
		} else if(event.gui instanceof GuiOptions) {
			GuiOptions go = (GuiOptions)event.gui;
			GuiCustomOptions gco = new GuiCustomOptions(GuiCustomOptions.getGuiScreen(go), GuiCustomOptions.getGameSettings(go));
			event.gui = gco;
		}
		
		else if(event.gui instanceof GuiChat || event.gui instanceof GuiInventory) {
			if(ReplayHandler.replayActive()) {
				event.setCanceled(true);
			}
		}
		
		else if(event.gui instanceof GuiIngameMenu) {
			if(ReplayHandler.replayActive()) {
				event.gui = new GuiExitReplay();
			}
		}
		
		else if(event.gui instanceof GuiDisconnected) {
			if(!ReplayHandler.replayActive() && System.currentTimeMillis() - ReplayHandler.lastExit < 5000) {
				event.setCanceled(true);
			}
		}
		
	}
	
}
