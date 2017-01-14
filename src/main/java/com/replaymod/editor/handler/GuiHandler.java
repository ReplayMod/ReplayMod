package com.replaymod.editor.handler;

import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiReplayEditor;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiHandler {
    private static final int BUTTON_REPLAY_EDITOR = 17890237;

    private final ReplayModEditor mod;

    public GuiHandler(ReplayModEditor mod) {
        this.mod = mod;
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(event.getGui() instanceof GuiMainMenu)) {
            return;
        }

        GuiButton button = new GuiButton(BUTTON_REPLAY_EDITOR, event.getGui().width / 2 + 2,
                event.getGui().height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayeditor"));
        button.width = button.width / 2 - 2;
        event.getButtonList().add(button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!event.getButton().enabled) return;

        if (event.getGui() instanceof GuiMainMenu) {
            if (event.getButton().id == BUTTON_REPLAY_EDITOR) {
                new GuiReplayEditor(GuiScreen.wrap(event.getGui()), mod.getCore()).display();
            }
        }
    }
}
