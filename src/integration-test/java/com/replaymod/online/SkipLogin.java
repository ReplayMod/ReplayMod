package com.replaymod.online;

import com.replaymod.core.AbstractTask;
import com.replaymod.online.gui.GuiLoginPrompt;

public class SkipLogin extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiLoginPrompt.class, gui -> {
            click(gui.cancelButton);
            expectGuiClosed(() -> future.set(null));
        });
    }
}
