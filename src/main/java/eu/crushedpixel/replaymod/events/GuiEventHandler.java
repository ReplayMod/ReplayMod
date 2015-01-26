package eu.crushedpixel.replaymod.events;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import eu.crushedpixel.replaymod.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.gui.replaymanager.GuiReplayManager;
import eu.crushedpixel.replaymod.gui.replaymanager.ResourceHelper;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.renderer.SafeEntityRenderer;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class GuiEventHandler {

	private static Minecraft mc = Minecraft.getMinecraft();
	
	private static List<Class> allowedGUIs = new ArrayList<Class>() {
		{
			add(GuiReplaySettings.class);
			add(GuiReplaySaving.class);
			add(GuiIngameMenu.class);
			add(GuiOptions.class);
			add(GuiVideoSettings.class);
		}
	};
	
	@SubscribeEvent
	public void onGui(GuiOpenEvent event) {
		if(!AuthenticationHandler.isAuthenticated()) return;
		if(event.gui != null && GuiReplaySaving.replaySaving && !allowedGUIs.contains(event.gui.getClass())) {
			event.gui = new GuiReplaySaving(event.gui);
			return;
		}
		if(!(event.gui instanceof GuiReplayManager)) ResourceHelper.freeResources();
		if(event.gui instanceof GuiChat || event.gui instanceof GuiInventory) {
			if(ReplayHandler.replayActive()) {
				event.setCanceled(true);
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
		if(event.gui instanceof GuiIngameMenu && ReplayHandler.replayActive()) {
			for(GuiButton b : new ArrayList<GuiButton>(event.buttonList)) {
				if(b.id == 1) {
					b.displayString = "Exit Replay";
					b.yPosition -= 24*2;
				} else if(b.id >= 5 && b.id <= 7) {
					event.buttonList.remove(b);
				} else if(b.id != 4) {
					b.yPosition -= 24*2;
				}
			}
		} else if(event.gui instanceof GuiMainMenu) {
			int i1 = event.gui.height / 4 + 48;

			for(GuiButton b : (List<GuiButton>)event.buttonList) {
				if(b.id != 0 && b.id != 4 && b.id != 5) {
					b.yPosition = b.yPosition - 24;
				}
			}

			GuiButton rm = new GuiButton(REPLAY_MANAGER_ID, event.gui.width / 2 - 100, i1 + 2*24, I18n.format("Replay Manager", new Object[0]));
			rm.enabled = AuthenticationHandler.isAuthenticated();
			event.buttonList.add(rm);
		} else if(event.gui instanceof GuiOptions) {
			event.buttonList.add(new GuiButton(9001, event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6 - 24, 310, 20, "Replay Mod Settings..."));
		}
	}

	@SubscribeEvent
	public void onButton(ActionPerformedEvent event) {
		if(!AuthenticationHandler.isAuthenticated()) return;
		if(event.gui instanceof GuiMainMenu && event.button.id == REPLAY_MANAGER_ID) {
			mc.displayGuiScreen(new GuiReplayManager());
		} else if(event.gui instanceof GuiOptions && event.button.id == REPLAY_OPTIONS_ID) {
			mc.displayGuiScreen(new GuiReplaySettings(event.gui));
		}
		
		if(ReplayHandler.replayActive() && event.gui instanceof GuiIngameMenu && event.button.id == 1) {
			event.button.enabled = false;
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {			
					//mc.displayGuiScreen(new GuiReplaySaving(new GuiMainMenu()));

					ReplayHandler.setSpeed(1f);
					ReplayHandler.endReplay();

					mc.gameSettings.setOptionFloatValue(Options.GAMMA, ReplayHandler.getInitialGamma());
					
					ReplayHandler.lastExit = System.currentTimeMillis();
					mc.theWorld.sendQuittingDisconnectingPacket();
					
					ReplayGuiRegistry.show();
				}
			});		
			t.run();
		}
	}

}
