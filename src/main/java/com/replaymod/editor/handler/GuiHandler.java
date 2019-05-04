package com.replaymod.editor.handler;

import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.utils.Utils;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiEditReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import net.minecraft.crash.CrashReport;

//#if MC>=11400
//$$ import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
//$$ import net.minecraft.client.gui.Screen;
//$$ import net.minecraft.client.gui.widget.AbstractButtonWidget;
//$$ import java.util.List;
//#else
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#endif

import java.io.IOException;

import static com.replaymod.core.versions.MCVer.*;

public class GuiHandler extends EventRegistrations {
    //#if MC>=11400
    //$$ { on(InitScreenCallback.EVENT, this::injectIntoReplayViewer); }
    //$$ public void injectIntoReplayViewer(Screen vanillaGuiScreen, List<AbstractButtonWidget> buttonList) {
    //#else
    @SubscribeEvent
    public void injectIntoReplayViewer(GuiScreenEvent.InitGuiEvent.Post event) {
        net.minecraft.client.gui.GuiScreen vanillaGuiScreen = getGui(event);
    //#endif
        AbstractGuiScreen guiScreen = GuiScreen.from(vanillaGuiScreen);
        if (!(guiScreen instanceof GuiReplayViewer)) {
            return;
        }
        final GuiReplayViewer replayViewer = (GuiReplayViewer) guiScreen;
        // Inject Edit button
        if (!replayViewer.editorButton.getChildren().isEmpty()) return;
        replayViewer.replaySpecificButtons.add(new GuiButton(replayViewer.editorButton).onClick(() -> {
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
        }).setSize(73, 20).setI18nLabel("replaymod.gui.edit").setDisabled());
    }
}
