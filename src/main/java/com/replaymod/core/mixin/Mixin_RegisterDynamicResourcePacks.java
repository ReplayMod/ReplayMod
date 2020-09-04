//#if FABRIC>=1
package com.replaymod.core.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

//#if MC>=11600
import net.minecraft.resource.ResourcePackSource;
//#else
//$$ import java.util.Map;
//#endif

@Mixin(ClientBuiltinResourcePackProvider.class)
public class Mixin_RegisterDynamicResourcePacks {
    @Inject(method = "register", at = @At("RETURN"))
    //#if MC>=11600
    private void registerReplayMod(Consumer<ResourcePackProfile> consumer, ResourcePackProfile.Factory factory, CallbackInfo ci) {
    //#else
    //$$ private <T extends ResourcePackProfile> void registerReplayMod(Map<String, T> map, ResourcePackProfile.Factory<T> factory, CallbackInfo ci) {
    //$$     Consumer<T> consumer = (pack) -> map.put(pack.getName(), pack);
    //#endif

        consumer.accept(ResourcePackProfile.of(
                LangResourcePack.NAME,
                true,
                LangResourcePack::new,
                factory,
                ResourcePackProfile.InsertionPosition.BOTTOM
                //#if MC>=11600
                , ResourcePackSource.PACK_SOURCE_BUILTIN
                //#endif
        ));

        if (ReplayMod.jGuiResourcePack != null) {
            consumer.accept(ResourcePackProfile.of(
                    ReplayMod.JGUI_RESOURCE_PACK_NAME,
                    true,
                    () -> ReplayMod.jGuiResourcePack,
                    factory,
                    ResourcePackProfile.InsertionPosition.BOTTOM
                    //#if MC>=11600
                    , ResourcePackSource.PACK_SOURCE_BUILTIN
                    //#endif
            ));
        }
    }
}
//#endif
