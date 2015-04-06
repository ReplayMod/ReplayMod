package eu.crushedpixel.replaymod.events;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.GuiCancelRender;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.gui.online.GuiLoginPrompt;
import eu.crushedpixel.replaymod.gui.online.GuiReplayCenter;
import eu.crushedpixel.replaymod.gui.online.GuiUploadFile;
import eu.crushedpixel.replaymod.gui.replaystudio.GuiReplayStudio;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.registry.LightingHandler;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.studio.VersionValidator;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.ResourceHelper;
import eu.crushedpixel.replaymod.video.VideoWriter;

public class GuiEventHandler {

	private static Minecraft mc = Minecraft.getMinecraft();

	private static int replayCount = 0;
	
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
		if(VideoWriter.isRecording() && !(event.gui instanceof GuiCancelRender)) {
			event.gui = null;
			return;
		}

		if(!(event.gui instanceof GuiReplayViewer || event.gui instanceof GuiUploadFile)) ResourceHelper.freeAllResources();

		if(event.gui instanceof GuiMainMenu) {
			if(ReplayMod.firstMainMenu) {
				ReplayMod.firstMainMenu = false;
				event.gui = new GuiLoginPrompt(event.gui, event.gui);
				return;
			} else {
				try {
					MCTimerHandler.setTimerSpeed(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if(!AuthenticationHandler.isAuthenticated()) return;
		if(event.gui != null && GuiReplaySaving.replaySaving && !allowedGUIs.contains(event.gui.getClass())) {
			event.gui = new GuiReplaySaving(event.gui);
			return;
		}
		if(event.gui instanceof GuiChat || event.gui instanceof GuiInventory) {
			if(ReplayHandler.isInReplay()) {
				event.setCanceled(true);
			}
		}

		else if(event.gui instanceof GuiDisconnected) {
			if(!ReplayHandler.isInReplay() && System.currentTimeMillis() - ReplayHandler.lastExit < 5000) {
				event.setCanceled(true);
			}
		}
	}

	private static final Color DARK_RED = Color.decode("#DF0101");
	private static final Color DARK_GREEN = Color.decode("#01DF01");

	@SubscribeEvent
	public void onDraw(DrawScreenEvent e) {
		if(e.gui instanceof GuiMainMenu) {
			e.gui.drawString(mc.fontRendererObj, "Replay Mod:", 5, 5, Color.WHITE.getRGB());
			if(AuthenticationHandler.isAuthenticated()) {
				e.gui.drawString(mc.fontRendererObj, "LOGGED IN", 5, 15, DARK_GREEN.getRGB());
			} else {
				e.gui.drawString(mc.fontRendererObj, "LOGGED OUT", 5, 15, DARK_RED.getRGB());
			}
			
			if(replayCount == 0) {
				if(editorButton.isMouseOver()) {
					Point mouse = MouseUtils.getMousePos();
					e.gui.drawCenteredString(mc.fontRendererObj, "At least one Replay required", (int)mouse.getX(), (int)mouse.getY()+4, Color.RED.getRGB());
				}
			} else if(!VersionValidator.isValid) {
				if(editorButton.isMouseOver()) {
					Point mouse = MouseUtils.getMousePos();
					e.gui.drawCenteredString(mc.fontRendererObj, "Java 1.7 or newer required", (int)mouse.getX(), (int)mouse.getY()+4, Color.RED.getRGB());
				}
			}
		}
	}

	private GuiButton editorButton;
	
	@SubscribeEvent
	public void onInit(InitGuiEvent event) {
		if(event.gui instanceof GuiIngameMenu && ReplayHandler.isInReplay()) {
			for(GuiButton b : new ArrayList<GuiButton>(event.buttonList)) {
				if(b.id == 1) {
					b.displayString = "Exit Replay";
					b.yPosition -= 24*2;
					b.id = GuiConstants.EXIT_REPLAY_BUTTON;
				} else if(b.id >= 5 && b.id <= 7) {
					event.buttonList.remove(b);
				} else if(b.id != 4) {
					b.yPosition -= 24*2;
				}
			}
		} else if(event.gui instanceof GuiMainMenu) {
			int i1 = event.gui.height / 4 + 24 + 10;

			for(GuiButton b : (List<GuiButton>)event.buttonList) {
				if(b.id != 0 && b.id != 4 && b.id != 5) {
					b.yPosition = b.yPosition - 2*24 + 10;
				}
			}

			GuiButton rm = new GuiButton(GuiConstants.REPLAY_MANAGER_BUTTON_ID, event.gui.width / 2 - 100, i1 + 2*24, "Replay Viewer");
			rm.width = rm.width/2 - 2;
			//rm.enabled = AuthenticationHandler.isAuthenticated();
			event.buttonList.add(rm);

			replayCount = ReplayFileIO.getAllReplayFiles().size();
			
			GuiButton re = new GuiButton(GuiConstants.REPLAY_EDITOR_BUTTON_ID, event.gui.width / 2 + 2, i1 + 2*24, "Replay Editor");
			re.width = re.width/2 - 2;
			re.enabled = VersionValidator.isValid && replayCount > 0;
			event.buttonList.add(re);
			
			editorButton = re;
			
			GuiButton rc = new GuiButton(GuiConstants.REPLAY_CENTER_BUTTON_ID, event.gui.width / 2 - 100, i1 + 3*24, "Replay Center");
			rc.enabled = true;
			event.buttonList.add(rc);
			
		} else if(event.gui instanceof GuiOptions) {
			event.buttonList.add(new GuiButton(GuiConstants.REPLAY_OPTIONS_BUTTON_ID, 
					event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6 - 24, 310, 20, "Replay Mod Settings..."));
		}
	}

	@SubscribeEvent
	public void onButton(ActionPerformedEvent event) {
		if(!event.button.enabled) return;
		if(event.gui instanceof GuiMainMenu) {
			if(event.button.id == GuiConstants.REPLAY_MANAGER_BUTTON_ID) {
				mc.displayGuiScreen(new GuiReplayViewer());
			} else if(event.button.id == GuiConstants.REPLAY_CENTER_BUTTON_ID) {
				if(AuthenticationHandler.isAuthenticated()) {
					mc.displayGuiScreen(new GuiReplayCenter());
				} else {
					mc.displayGuiScreen(new GuiLoginPrompt(event.gui, new GuiReplayCenter()));
				}
			} else if(event.button.id == GuiConstants.REPLAY_EDITOR_BUTTON_ID) {
				mc.displayGuiScreen(new GuiReplayStudio());
			}
		} else if(event.gui instanceof GuiOptions && event.button.id == GuiConstants.REPLAY_OPTIONS_BUTTON_ID) {
			mc.displayGuiScreen(new GuiReplaySettings(event.gui));
		}

		if(ReplayHandler.isInReplay() && event.gui instanceof GuiIngameMenu && event.button.id == GuiConstants.EXIT_REPLAY_BUTTON) {
			if(ReplayHandler.isInPath()) ReplayProcess.stopReplayProcess(false);
			ReplayHandler.endReplay();
			
			event.button.enabled = false;
			
			LightingHandler.setLighting(false);

			ReplayHandler.lastExit = System.currentTimeMillis();

			mc.theWorld.sendQuittingDisconnectingPacket();
			mc.loadWorld((WorldClient)null);
			mc.displayGuiScreen(new GuiMainMenu());
			
			ReplayGuiRegistry.show();
		}
	}

}
