package com.replaymod.simplepathing.preview;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.Setting;

//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import org.lwjgl.input.Keyboard;
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class PathPreview {
    private final ReplayModSimplePathing mod;

    private ReplayHandler replayHandler;
    private PathPreviewRenderer renderer;

    public PathPreview(ReplayModSimplePathing mod) {
        this.mod = mod;
    }

    public void register() {
        FML_BUS.register(this);
    }

    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.pathpreview", Keyboard.KEY_H, () -> {
            SettingsRegistry settings = mod.getCore().getSettingsRegistry();
            settings.set(Setting.PATH_PREVIEW, !settings.get(Setting.PATH_PREVIEW));
            settings.save();
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
