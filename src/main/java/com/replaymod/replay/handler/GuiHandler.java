package com.replaymod.replay.handler;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiHandler {
    private static final int BUTTON_EXIT_SERVER = 1;
    private static final int BUTTON_ACHIEVEMENTS = 5;
    private static final int BUTTON_STATS = 6;
    private static final int BUTTON_OPEN_TO_LAN = 7;

    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ReplayModReplay mod;

    public GuiHandler(ReplayModReplay mod) {
        this.mod = mod;
    }

    public void register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoIngameMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiIngameMenu)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            GuiButton achievements = null, stats = null, openToLan = null;
            @SuppressWarnings("unchecked")
            List<GuiButton> buttonList = event.buttonList;
            for(GuiButton b : new ArrayList<>(buttonList)) {
                switch (b.id) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    case BUTTON_EXIT_SERVER:
                        b.displayString = I18n.format("replaymod.gui.exit");
                        b.id = BUTTON_EXIT_REPLAY;
                        break;
                    // Remove "Achievements", "Stats" and "Open to LAN" buttons
                    case BUTTON_ACHIEVEMENTS:
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
                moveAllButtonsDirectlyBelowUpwards(buttonList, achievements.yPosition,
                        achievements.xPosition, stats.xPosition + stats.width);
            }
            if (openToLan != null) {
                moveAllButtonsDirectlyBelowUpwards(buttonList, openToLan.yPosition,
                        openToLan.xPosition, openToLan.xPosition + openToLan.width);
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
            if (button.yPosition >= belowY && button.xPosition <= xEnd && button.xPosition + button.width >= xStart) {
                button.yPosition -= 24;
            }
        }
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = event.buttonList;
        GuiButton button = new GuiButton(BUTTON_REPLAY_VIEWER, event.gui.width / 2 - 100,
                event.gui.height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayviewer"));
        button.width = button.width / 2 - 2;
        buttonList.add(button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!event.button.enabled) return;

        if (event.gui instanceof GuiMainMenu) {
            if (event.button.id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
            }
        }

        if (event.gui instanceof GuiIngameMenu && mod.getReplayHandler() != null) {
            if (event.button.id == BUTTON_EXIT_REPLAY) {
                event.button.enabled = false;
                mc.displayGuiScreen(new GuiMainMenu());
                try {
                    mod.getReplayHandler().endReplay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
