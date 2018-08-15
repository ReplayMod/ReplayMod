package com.replaymod.replay.handler;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// RAH Start
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import java.io.File; // RAH
import java.io.FileFilter; // RAH
import java.io.IOException; // RAH 
import org.apache.commons.io.FileUtils; // RAH
import org.apache.commons.io.IOCase; // RAH
import org.apache.commons.io.filefilter.SuffixFileFilter; // RAH
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
// RAH end

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

public class GuiHandler {
    private static final int BUTTON_EXIT_SERVER = 1;
    private static final int BUTTON_ADVANCEMENTS = 5;
    private static final int BUTTON_STATS = 6;
    private static final int BUTTON_OPEN_TO_LAN = 7;

    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ReplayModReplay mod;

	// RAH begin
	private   GuiReplayViewer guiReplayViewer;
	//RAH end

    public GuiHandler(ReplayModReplay mod) {
        this.mod = mod;
    }

    public void register() {
        FML_BUS.register(this);
        FORGE_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoIngameMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(getGui(event) instanceof GuiIngameMenu)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            GuiButton achievements = null, stats = null, openToLan = null;
            List<GuiButton> buttonList = getButtonList(event);
            for(GuiButton b : new ArrayList<>(buttonList)) {
                switch (b.id) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    case BUTTON_EXIT_SERVER:
                        b.displayString = I18n.format("replaymod.gui.exit");
                        b.id = BUTTON_EXIT_REPLAY;
                        break;
                    // Remove "Advancements", "Stats" and "Open to LAN" buttons
                    case BUTTON_ADVANCEMENTS:
                        buttonList.remove(achievements = b);
                        break;
                    case BUTTON_STATS:
                        buttonList.remove(stats = b);
                        break;
                    case BUTTON_OPEN_TO_LAN:
                        buttonList.remove(openToLan = b);
                        break;
                }
            }
            if (achievements != null && stats != null) {
                moveAllButtonsDirectlyBelowUpwards(buttonList, y(achievements),
                        x(achievements), x(stats) + stats.width);
            }
            if (openToLan != null) {
                moveAllButtonsDirectlyBelowUpwards(buttonList, y(openToLan),
                        x(openToLan), x(openToLan) + openToLan.width);
            }
        }
    }

    /**
     * Moves all buttons that are within a rectangle below a certain y coordinate upwards by 24 units.
     * @param buttons List of buttons
     * @param belowY The Y limit
     * @param xStart Left x limit of the rectangle
     * @param xEnd Right x limit of the rectangle
     */
    private void moveAllButtonsDirectlyBelowUpwards(List<GuiButton> buttons, int belowY, int xStart, int xEnd) {
        for (GuiButton button : buttons) {
            if (y(button) >= belowY && x(button) <= xEnd && x(button) + button.width >= xStart) {
                y(button, y(button) - 24);
            }
        }
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(getGui(event) instanceof GuiMainMenu)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Something went terribly wrong and we ended up in the main menu with the replay still active.
            // To prevent players from joining live servers and using the CameraEntity, try to stop the replay now.
            try {
                mod.getReplayHandler().endReplay();
            } catch (IOException e) {
                LOGGER.error("Trying to stop broken replay: ", e);
            } finally {
                if (mod.getReplayHandler() != null) {
                    mod.forcefullyStopReplay();
                }
            }
        }

        GuiButton button = new GuiButton(BUTTON_REPLAY_VIEWER, getGui(event).width / 2 - 100,
                getGui(event).height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayviewer"));
        button.width = button.width / 2 - 2;
        getButtonList(event).add(button);
		//processFile(); // RAH Can we call this directly from the main menu?
    }

	// RAH 
	/**
	* Delay initiating the load button so that things can come up operationally
	*
	**/
	private void delayedClick(int delay_ms)
	{
		LOGGER.debug("GuiHandler.delayedClick()" + delay_ms);
		if (delay_ms > 0) {
			new Thread(() -> {
				try {
					Thread.sleep(delay_ms);
				} catch (InterruptedException e) {
					LOGGER.debug(e);
					return;
				}
				LOGGER.debug("calling guiReplayViewer.loadButton");
				guiReplayViewer.loadButton.onClick();
			}).start(); // End of thread
		}
	}

	/**
	* RAH - There must be 1 and only 1 .mcpr file in the directory - otherwise things get whacky - we'll address this later
	*
	**/
	public void processFile()
	{
		LOGGER.debug("Process Single File");
		//try {
		//	Thread.sleep(500);
		//} catch (InterruptedException e) {
		//	LOGGER.debug(e);
		//	return;
		//}
        try {
			File folder = mod.getCore().getReplayFolder();
            for (final File file : folder.listFiles((FileFilter) new SuffixFileFilter(".mcpr", IOCase.INSENSITIVE))) {
                if (Thread.interrupted()) break;
				LOGGER.debug("mod.startReplay("+file+")");
				mod.startReplay(file); // RAH - this returns after setup, so we can't loop through all the files - just one at a time
			}
        } catch (Exception e) {
            e.printStackTrace();
        }	
	}

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
		LOGGER.debug("Replay Viewer Called");
        if(!getButton(event).enabled) return;

        if (getGui(event) instanceof GuiMainMenu) {
            if (getButton(event).id == BUTTON_REPLAY_VIEWER) {
				LOGGER.debug("Main Menu Replay Viewer Button Press");
				// RAH - we can bypass guiReplayViewer completely
				//guiReplayViewer = new GuiReplayViewer(mod);
				//guiReplayViewer.display(); // RAH - added variable and made it a member variable
				processFile();
				// RAH, this doesn't work because we are not the MC thread.
				//delayedClick(5000); // RAH - after a few seconds, load the selected item - which is the first file
				//guiReplayViewer.loadButton.onClick();
            }
        }

        if (getGui(event) instanceof GuiIngameMenu && mod.getReplayHandler() != null) {
            if (getButton(event).id == BUTTON_EXIT_REPLAY) {
                getButton(event).enabled = false;
                try {
                    mod.getReplayHandler().endReplay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
