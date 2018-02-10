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

import static com.replaymod.core.versions.MCVer.*;

public class GuiHandler {
    private static final int BUTTON_REPLAY_EDITOR = 17890237;

    private final ReplayModEditor mod;

    public GuiHandler(ReplayModEditor mod) {
        this.mod = mod;
    }

    public void register() {
        FML_BUS.register(this);
        FORGE_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(getGui(event) instanceof GuiMainMenu)) {
            return;
        }

        GuiButton button = new GuiButton(BUTTON_REPLAY_EDITOR, getGui(event).width / 2 + 2,
                getGui(event).height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayeditor"));
        button.width = button.width / 2 - 2;
        getButtonList(event).add(button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!getButton(event).enabled) return;

        if (getGui(event) instanceof GuiMainMenu) {
            if (getButton(event).id == BUTTON_REPLAY_EDITOR) {
                new GuiReplayEditor(GuiScreen.wrap(getGui(event)), mod.getCore()).display();
            }
        }
    }
}
