package eu.crushedpixel.replaymod.events;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import eu.crushedpixel.replaymod.gui.GuiCustomOptions;
import eu.crushedpixel.replaymod.gui.GuiExitReplay;
import eu.crushedpixel.replaymod.gui.GuiReplayManager;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class GuiEventHandler {

	@SubscribeEvent
	public void onGui(GuiOpenEvent event) {
		if(event.gui instanceof GuiChat || event.gui instanceof GuiInventory) {
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

	private static final int REPLAY_MANAGER_ID = 9001;
	private static final int REPLAY_OPTIONS_ID = 9001;

	@SubscribeEvent
	public void onInit(InitGuiEvent event) {
		if(event.gui instanceof GuiMainMenu) {
			int i1 = event.gui.height / 4 + 48;

			for(GuiButton b : (List<GuiButton>)event.buttonList) {
				if(b.id != 0 && b.id != 4 && b.id != 5) {
					b.yPosition = b.yPosition - 24;
				}
			}

			event.buttonList.add(new GuiButton(REPLAY_MANAGER_ID, event.gui.width / 2 - 100, i1 + 2*24, I18n.format("Replay Manager", new Object[0])));
		} else if(event.gui instanceof GuiOptions) {
			event.buttonList.add(new GuiButton(9001, event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6 - 24, 310, 20, "Replay Mod Settings..."));
		}
	}
	
	@SubscribeEvent
	public void onButton(ActionPerformedEvent event) {
		if(event.gui instanceof GuiMainMenu && event.button.id == REPLAY_MANAGER_ID) {
			if(ConnectionEventHandler.saving) {
				Minecraft.getMinecraft().displayGuiScreen(new GuiReplaySaving());
			} else {
				Minecraft.getMinecraft().displayGuiScreen(new GuiReplayManager());
			}
		} else if(event.gui instanceof GuiOptions && event.button.id == REPLAY_OPTIONS_ID) {
			Minecraft.getMinecraft().displayGuiScreen(new GuiReplaySettings(event.gui));
		}
	}

}
