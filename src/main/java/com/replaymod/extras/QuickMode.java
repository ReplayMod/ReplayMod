package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.IGuiImage;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;

public class QuickMode extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 40, 100, 16, 16).setSize(16, 16);

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.quickmode", Keyboard.KEY_Q, new Runnable() {
            @Override
            public void run() {
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
            }
        });

        register();
    }

    {
        on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay(), replayHandler.isQuickMode()));
    }

    private void updateIndicator(GuiReplayOverlay overlay, boolean enabled) {
        if (enabled) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }
}
