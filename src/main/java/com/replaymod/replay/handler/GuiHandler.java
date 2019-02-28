package com.replaymod.replay.handler;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

public class GuiHandler {
    private static final int BUTTON_EXIT_SERVER = 1;
    private static final int BUTTON_ADVANCEMENTS = 5;
    private static final int BUTTON_STATS = 6;
    private static final int BUTTON_OPEN_TO_LAN = 7;

    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private static final Minecraft mc = MCVer.getMinecraft();

    private final ReplayModReplay mod;

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
                        removeButton(event, b);
                        addButton(event, new GuiButton(BUTTON_EXIT_REPLAY, b.x, b.y, b.width, b.height, I18n.format("replaymod.gui.exit")) {
                            //#if MC>=11300
                            @Override
                            public void onClick(double mouseX, double mouseY) {
                                onButton(new GuiScreenEvent.ActionPerformedEvent.Pre(getGui(event), this, new ArrayList<>()));
                            }
                            //#endif
                        });
                        break;
                    // Remove "Advancements", "Stats" and "Open to LAN" buttons
                    case BUTTON_ADVANCEMENTS:
                        removeButton(event, achievements = b);
                        break;
                    case BUTTON_STATS:
                        removeButton(event, stats = b);
                        break;
                    case BUTTON_OPEN_TO_LAN:
                        removeButton(event, openToLan = b);
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
    public void ensureReplayStopped(GuiScreenEvent.InitGuiEvent event) {
        if (!(getGui(event) instanceof GuiMainMenu || getGui(event) instanceof GuiMultiplayer)) {
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
    }


    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(getGui(event) instanceof GuiMainMenu)) {
            return;
        }
        GuiButton button = new GuiButton(BUTTON_REPLAY_VIEWER, getGui(event).width / 2 - 100,
                getGui(event).height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayviewer")) {
            //#if MC>=11300
            @Override
            public void onClick(double mouseX, double mouseY) {
                onButton(new GuiScreenEvent.ActionPerformedEvent.Pre(getGui(event), this, new ArrayList<>()));
            }
            //#endif
        };
        button.width = button.width / 2 - 2;
        addButton(event, button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!getButton(event).enabled) return;

        if (getGui(event) instanceof GuiMainMenu) {
            if (getButton(event).id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
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
