//#if FABRIC>=1
package com.replaymod.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

// See ReplayModMMLauncher. This is the fallback if MM is not installed.
public class ReplayModNonMMLauncher implements IMixinConfigPlugin {
    private final Logger logger = LogManager.getLogger("replaymod/nonmm");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        try {
            if (ReplayModMixinConfigPlugin.hasClass("com.chocohead.mm.Plugin")) {
                logger.info("Detected MM, they should call us...");
            } else {
                logger.info("Did not detect MM, initializing ourselves...");
                new ReplayModMMLauncher().run();
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
//#endif
