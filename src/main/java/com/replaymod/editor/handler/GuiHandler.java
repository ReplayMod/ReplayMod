package com.replaymod.editor.handler;

import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiReplayEditor;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class GuiHandler {
    private static final int BUTTON_REPLAY_EDITOR = 17890237;

    private final ReplayModEditor mod;

    public GuiHandler(ReplayModEditor mod) {
        this.mod = mod;
    }

    public void register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = event.buttonList;
        GuiButton button = new GuiButton(BUTTON_REPLAY_EDITOR, event.gui.width / 2 + 2,
                event.gui.height / 4 + 10 + 3 * 24, I18n.format("replaymod.gui.replayeditor"));
        button.width = button.width / 2 - 2;
        buttonList.add(button);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!event.button.enabled) return;

        if (event.gui instanceof GuiMainMenu) {
            if (event.button.id == BUTTON_REPLAY_EDITOR) {
                new GuiReplayEditor(GuiScreen.wrap(event.gui), mod.getCore()).display();
            }
        }
    }
}
