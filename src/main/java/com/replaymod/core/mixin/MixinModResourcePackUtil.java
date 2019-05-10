//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.LangResourcePack;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resources.ModResourcePackUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ModResourcePackUtil.class, remap = false)
public class MixinModResourcePackUtil {
    @Inject(method = "appendModResourcePacks", at = @At("RETURN"), remap = false)
    private static void injectRMLangPack(List<ResourcePack> packList, ResourceType type, CallbackInfo ci) {
        if (type != ResourceType.ASSETS) return;

        for (int i = 0; i < packList.size(); i++) {
            ResourcePack pack = packList.get(i);
            if (pack instanceof ModResourcePack && ((ModResourcePack) pack).getFabricModMetadata().getId().equals(ReplayMod.MOD_ID)) {
                ModContainer container = FabricLoader.getInstance().getModContainer(ReplayMod.MOD_ID).orElseThrow(IllegalAccessError::new);
                packList.add(i, new LangResourcePack(container.getRootPath()));
                return;
            }
        }

        throw new IllegalStateException("Could not find ReplayMod resource pack.");
    }
}
//#endif
