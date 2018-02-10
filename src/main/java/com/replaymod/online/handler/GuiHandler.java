package com.replaymod.online.handler;

import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.gui.GuiLoginPrompt;
import com.replaymod.online.gui.GuiReplayCenter;
import com.replaymod.online.gui.GuiUploadReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiElement;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

import static com.replaymod.core.versions.MCVer.*;

public class GuiHandler {
    private static final int BUTTON_REPLAY_CENTER = 17890236;

    private final ReplayModOnline mod;

    public GuiHandler(ReplayModOnline mod) {
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

        GuiButton button = new GuiButton(BUTTON_REPLAY_CENTER, getGui(event).width / 2 - 100,
                getGui(event).height / 4 + 10 + 4 * 24, I18n.format("replaymod.gui.replaycenter"));
        getButtonList(event).add(button);
    }

    @SubscribeEvent
    public void injectIntoReplayViewer(GuiScreenEvent.InitGuiEvent.Post event) {
        AbstractGuiScreen guiScreen = GuiScreen.from(getGui(event));
        if (!(guiScreen instanceof GuiReplayViewer)) {
            return;
        }
        final GuiReplayViewer replayViewer = (GuiReplayViewer) guiScreen;
        // Inject Upload button
        for (GuiElement element : replayViewer.replayButtonPanel.getChildren()) {
            if (element instanceof GuiPanel && (((GuiPanel) element).getChildren().isEmpty())) {
                new de.johni0702.minecraft.gui.element.GuiButton((GuiPanel) element).onClick(new Runnable() {
                    @Override
                    public void run() {
                        File replayFile = replayViewer.list.getSelected().file;
                        GuiUploadReplay uploadGui = new GuiUploadReplay(replayViewer, mod, replayFile);
                        if (mod.isLoggedIn()) {
                            uploadGui.display();
                        } else {
                            new GuiLoginPrompt(mod.getApiClient(), replayViewer, uploadGui, true);
                        }
                    }
                }).setSize(73, 20).setI18nLabel("replaymod.gui.upload").setDisabled();
            }
        }
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if(!getButton(event).enabled) return;

        if (getGui(event) instanceof GuiMainMenu) {
            if (getButton(event).id == BUTTON_REPLAY_CENTER) {
                GuiReplayCenter replayCenter = new GuiReplayCenter(mod);
                if (mod.isLoggedIn()) {
                    replayCenter.display();
                } else {
                    new GuiLoginPrompt(mod.getApiClient(), GuiScreen.wrap(getGui(event)), replayCenter, true).display();
                }
            }
        }
    }
}
