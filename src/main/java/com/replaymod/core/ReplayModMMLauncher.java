//#if FABRIC>=1
package com.replaymod.core;

import org.spongepowered.asm.mixin.Mixins;

// We need to wait for MM to call us, otherwise we might initialize our mixins before it calls optifabric which would
// result in OF classes not existing while our mixins get resolved.
public class ReplayModMMLauncher implements Runnable {
    @Override
    public void run() {
        Mixins.addConfiguration("mixins.compat.mapwriter.replaymod.json");
        Mixins.addConfiguration("mixins.compat.shaders.replaymod.json");
        Mixins.addConfiguration("mixins.core.replaymod.json");
        Mixins.addConfiguration("mixins.extras.playeroverview.replaymod.json");
        Mixins.addConfiguration("mixins.recording.replaymod.json");
        Mixins.addConfiguration("mixins.render.blend.replaymod.json");
        Mixins.addConfiguration("mixins.render.replaymod.json");
        Mixins.addConfiguration("mixins.replay.replaymod.json");
    }
}
//#endif
