package com.replaymod.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
//#endif

//#if MC>=11400
import java.io.InputStream;
//#else
//$$ import net.minecraft.launchwrapper.Launch;
//#endif

//#if MC>=11200
import org.objectweb.asm.tree.ClassNode;
//#else
//$$ import org.spongepowered.asm.lib.tree.ClassNode;
//#endif

public class ReplayModMixinConfigPlugin implements IMixinConfigPlugin {
    static boolean hasClass(String name) throws IOException {
        //#if MC>=11400
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream != null) stream.close();
        return stream != null;
        //#else
        //$$ return Launch.classLoader.getClassBytes(name) != null;
        //#endif
    }

    private final Logger logger = LogManager.getLogger("replaymod/mixin");
    private final boolean hasOF = hasClass("optifine.OptiFineForgeTweaker") || hasClass("me.modmuss50.optifabric.mod.Optifabric");
    //#if FABRIC
    private final boolean hasIris = FabricLoader.getInstance().isModLoaded("iris");
    //#endif

    {
        logger.debug("hasOF: " + hasOF);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (hasOF) {
            //#if MC>=11500
            // OF renames the lambda method name and I see no way we can target it now, so we give up on that patch
            if (mixinClassName.endsWith("MixinTileEntityEndPortalRenderer")) return false;
            //#endif
        }
        if (mixinClassName.endsWith("_OF")) return hasOF;
        if (mixinClassName.endsWith("_NoOF")) return !hasOF;
        //#if FABRIC
        if (mixinClassName.endsWith("_Iris")) return hasIris;
        //#endif
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    public ReplayModMixinConfigPlugin() throws IOException {
    }
}
