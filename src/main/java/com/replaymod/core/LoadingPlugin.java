package com.replaymod.core;

import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;

public class LoadingPlugin implements IFMLLoadingPlugin {

    public LoadingPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.recording.replaymod.json");
        Mixins.addConfiguration("mixins.render.replaymod.json");
        Mixins.addConfiguration("mixins.replay.replaymod.json");
        Mixins.addConfiguration("mixins.compat.mapwriter.replaymod.json");
        Mixins.addConfiguration("mixins.compat.shaders.replaymod.json");
        Mixins.addConfiguration("mixins.extras.playeroverview.replaymod.json");

        CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                File file = new File(location.toURI());
                if (file.isFile()) {
                    // This forces forge to reexamine the jar file for FML mods
                    // Should eventually be handled by Mixin itself, maybe?
                    CoreModManager.getIgnoredMods().remove(file.getName());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            LogManager.getLogger().warn("No CodeSource, if this is not a development environment we might run into problems!");
            LogManager.getLogger().warn(getClass().getProtectionDomain());
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
