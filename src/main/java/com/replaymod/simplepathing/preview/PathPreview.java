package com.replaymod.simplepathing.preview;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.Setting;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class PathPreview {
    private final ReplayModSimplePathing mod;

    private ReplayHandler replayHandler;
    private PathPreviewRenderer renderer;

    public PathPreview(ReplayModSimplePathing mod) {
        this.mod = mod;
    }

    public void register() {
        FMLCommonHandler.instance().bus().register(this);

        ReplayMod core = mod.getCore();
        mod.getCore().getKeyBindingRegistry().registerKeyBinding("replaymod.input.pathpreview", Keyboard.KEY_H, () -> {
            SettingsRegistry registry = core.getSettingsRegistry();
            registry.set(Setting.PATH_PREVIEW, !registry.get(Setting.PATH_PREVIEW));
            registry.save();
        });
    }

    @SubscribeEvent
    public void onReplayOpen(ReplayOpenEvent.Post event) {
        replayHandler = event.getReplayHandler();
        update();
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Pre event) {
        replayHandler = null;
        update();
    }

    @SubscribeEvent
    public void onSettingsChanged(SettingsChangedEvent event) {
        if (event.getKey() == Setting.PATH_PREVIEW) {
            update();
        }
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
