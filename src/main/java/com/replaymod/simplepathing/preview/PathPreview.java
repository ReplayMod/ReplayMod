package com.replaymod.simplepathing.preview;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.SettingsChangedCallback;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.Setting;

public class PathPreview extends EventRegistrations {
    private final ReplayModSimplePathing mod;

    private ReplayHandler replayHandler;
    private PathPreviewRenderer renderer;

    public PathPreview(ReplayModSimplePathing mod) {
        this.mod = mod;

        on(SettingsChangedCallback.EVENT, (registry, key) -> {
            if (key == Setting.PATH_PREVIEW) {
                update();
            }
        });

        on(ReplayOpenedCallback.EVENT, replayHandler -> {
            this.replayHandler = replayHandler;
            update();
        });

        on(ReplayClosedCallback.EVENT, replayHandler -> {
            this.replayHandler = null;
            update();
        });
    }

    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.pathpreview", Keyboard.KEY_H, () -> {
            SettingsRegistry settings = mod.getCore().getSettingsRegistry();
            settings.set(Setting.PATH_PREVIEW, !settings.get(Setting.PATH_PREVIEW));
            settings.save();
        });
    }

    private void update() {
        if (mod.getCore().getSettingsRegistry().get(Setting.PATH_PREVIEW) && replayHandler != null) {
            if (renderer == null) {
                renderer = new PathPreviewRenderer(mod, replayHandler);
                renderer.register();
            }
        } else {
            if (renderer != null) {
                renderer.unregister();
                renderer = null;
            }
        }
    }
}
