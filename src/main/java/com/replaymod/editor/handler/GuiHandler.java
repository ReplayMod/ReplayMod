package com.replaymod.editor.handler;

import com.replaymod.core.utils.Utils;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiEditReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiElement;
import net.minecraft.crash.CrashReport;
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

import static com.replaymod.core.versions.MCVer.*;

public class GuiHandler {
    public void register() {
        FML_BUS.register(this);
        FORGE_BUS.register(this);
    }

    @SubscribeEvent
    public void injectIntoReplayViewer(GuiScreenEvent.InitGuiEvent.Post event) {
        AbstractGuiScreen guiScreen = GuiScreen.from(getGui(event));
        if (!(guiScreen instanceof GuiReplayViewer)) {
            return;
        }
        final GuiReplayViewer replayViewer = (GuiReplayViewer) guiScreen;
        // Inject Edit button
        for (GuiElement element : replayViewer.replayButtonPanel.getChildren()) {
            if (element instanceof GuiPanel && (((GuiPanel) element).getChildren().isEmpty())) {
                new de.johni0702.minecraft.gui.element.GuiButton((GuiPanel) element).onClick(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new GuiEditReplay(replayViewer, replayViewer.list.getSelected().file.toPath()) {
                                @Override
                                protected void close() {
                                    super.close();
                                    replayViewer.list.load();
                                }
                            }.open();
                        } catch (IOException e) {
                            Utils.error(ReplayModEditor.LOGGER, replayViewer, CrashReport.makeCrashReport(e, "Opening replay editor"), () -> {});
                        }
                    }
                }).setSize(73, 20).setI18nLabel("replaymod.gui.replayeditor").setDisabled();
            }
        }
    }
}
