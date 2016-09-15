package com.replaymod.replay.handler;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.util.ArrayList;

public class GuiHandler {
    private static final int BUTTON_EXIT_SERVER = 1;
    private static final int BUTTON_RETURN_TO_GAME = 4;
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
        if (!(event.getGui() instanceof GuiIngameMenu)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            for(GuiButton b : new ArrayList<>(event.getButtonList())) {
                switch (b.id) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    case BUTTON_EXIT_SERVER:
                        b.displayString = I18n.format("replaymod.gui.exit");
                        b.id = BUTTON_EXIT_REPLAY;
                        break;
                    // Remove "Achievements", "Stats" and "Open to LAN" buttons
                    case BUTTON_ACHIEVEMENTS:
                    case BUTTON_STATS:
                    case BUTTON_OPEN_TO_LAN:
                        event.getButtonList().remove(b);
                }
                // Move all buttons except the "Return to game" button upwards
                if (b.id != BUTTON_RETURN_TO_GAME) {
                    b.yPosition -= 48;
                }
            }
        }
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(event.getGui() instanceof GuiMainMenu)) {
            return;
        }

        GuiButton button = new GuiButton(BUTTON_REPLAY_VIEWER, event.getGui().width / 2 - 100,
                event.getGui().height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayviewer"));
        button.width = button.width / 2 - 2;
        event.getButtonList().add(button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!event.getButton().enabled) return;

        if (event.getGui() instanceof GuiMainMenu) {
            if (event.getButton().id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
            }
        }

        if (event.getGui() instanceof GuiIngameMenu && mod.getReplayHandler() != null) {
            if (event.getButton().id == BUTTON_EXIT_REPLAY) {
                event.getButton().enabled = false;
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
