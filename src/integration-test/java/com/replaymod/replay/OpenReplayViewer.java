package com.replaymod.replay;

import com.replaymod.core.AbstractTask;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.GuiMainMenu;

public class OpenReplayViewer extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiMainMenu.class, mainMenu -> {
            click("Replay Viewer");
            expectGui(GuiReplayViewer.class, replayViewer -> future.set(null));
        });
    }
}
