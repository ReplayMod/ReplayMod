package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replay.gui.overlay.UIStatusIndicator;
import de.johni0702.minecraft.gui.utils.EventRegistrations;

public class QuickMode extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final UIStatusIndicator indicator = new UIStatusIndicator(40, 100);

    @Override
    public void register(final ReplayMod mod) {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.quickmode", Keyboard.KEY_Q, () -> {
            ReplayHandler replayHandler = module.getReplayHandler();
            if (replayHandler == null) {
                return;
            }
            replayHandler.getReplaySender().setSyncModeAndWait();
            mod.runLater(() -> {
                replayHandler.ensureQuickModeInitialized(() -> {
                    boolean enabled = !replayHandler.isQuickMode();
                    updateIndicator(replayHandler.getOverlay(), enabled);
                    replayHandler.setQuickMode(enabled);
                    replayHandler.getReplaySender().setAsyncMode(true);
                });
            });
        }, true);

        register();
    }

    {
        on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay(), replayHandler.isQuickMode()));
    }

    private void updateIndicator(GuiReplayOverlay overlay, boolean enabled) {
        if (enabled) {
            overlay.kt.getBottomLeftPanel().addChild(indicator);
        } else {
            overlay.kt.getBottomLeftPanel().removeChild(indicator);
        }
    }
}
