//#if FABRIC>=1
package com.replaymod.core;

import com.replaymod.extras.modcore.ModCoreInstaller;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.Optional;

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

        try {
            initModCore();
        } catch (Throwable t) {
            System.err.println("ReplayMod caught error during ModCore init:");
            t.printStackTrace();
        }
    }

    // Forge equivalent is in ReplayModTweaker
    private void initModCore() {
        if (System.getProperty("REPLAYMOD_SKIP_MODCORE", "false").equalsIgnoreCase("true")) {
            System.out.println("ReplayMod not initializing ModCore because REPLAYMOD_SKIP_MODCORE is true.");
            return;
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            System.out.println("ReplayMod not initializing ModCore because we're in a development environment.");
            return;
        }

        File gameDir = FabricLoader.getInstance().getGameDirectory();

        Optional<ModContainer> minecraft = FabricLoader.getInstance().getModContainer("minecraft");
        if (!minecraft.isPresent()) {
            System.err.println("ReplayMod could not determine Minecraft version, skipping ModCore.");
            return;
        }
        String mcVer = minecraft.get().getMetadata().getVersion().getFriendlyString();

        int result = ModCoreInstaller.initialize(gameDir, mcVer + "_fabric");
        if (result != -2) { // Don't even bother logging the result if there's no ModCore for this version.
            System.out.println("ReplayMod ModCore init result: " + result);
        }
        if (ModCoreInstaller.isErrored()) {
            System.err.println(ModCoreInstaller.getError());
        }
    }
}
//#endif
