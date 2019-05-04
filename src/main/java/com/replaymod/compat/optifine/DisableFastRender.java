package com.replaymod.compat.optifine;

import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.events.ReplayRenderCallback;
import net.minecraft.client.Minecraft;

public class DisableFastRender extends EventRegistrations {

    private final Minecraft mc = Minecraft.getInstance();

    private boolean wasFastRender = false;

    { on(ReplayRenderCallback.Pre.EVENT, renderer -> onRenderBegin()); }
    private void onRenderBegin() {
        if (!MCVer.hasOptifine()) return;

        try {
            wasFastRender = (boolean) OptifineReflection.gameSettings_ofFastRender.get(mc.gameSettings);
            OptifineReflection.gameSettings_ofFastRender.set(mc.gameSettings, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    { on(ReplayRenderCallback.Post.EVENT, renderer -> onRenderEnd()); }
    private void onRenderEnd() {
        if (!MCVer.hasOptifine()) return;

        try {
            OptifineReflection.gameSettings_ofFastRender.set(mc.gameSettings, wasFastRender);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
