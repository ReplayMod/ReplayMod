package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;

public class AdvancedScreenshots implements Extra {

    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) {
        this.mod = mod;
    }

    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if (instance != null) {
            instance.takeScreenshot();
        }
    }

    private void takeScreenshot() {
        ReplayMod.instance.runLater(() -> new GuiCreateScreenshot(mod).display());
    }
}
