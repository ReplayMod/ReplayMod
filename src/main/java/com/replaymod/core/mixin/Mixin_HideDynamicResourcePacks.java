//#if FABRIC>=1
package com.replaymod.core.mixin;

import net.minecraft.client.gui.screen.pack.PackListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11600
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
//#else
//$$ import net.minecraft.client.resource.ClientResourcePackProfile;
//#endif

//#if MC>=11600
@Mixin(PackScreen.class)
//#else
//$$ @Mixin(ResourcePackListWidget.class)
//#endif
public abstract class Mixin_HideDynamicResourcePacks {
    //#if MC>=11600
    @Inject(method = "method_29672", at = @At("HEAD"), cancellable = true)
    private void hideInternalPacks(PackListWidget packListWidget, ResourcePackOrganizer.Pack pack, CallbackInfo info) {
    //#else
    //$$ @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    //$$ private void hideInternalPacks(ResourcePackListWidget.ResourcePackEntry entry, CallbackInfo info) {
    //$$     ClientResourcePackProfile pack = entry.getPack();
    //#endif
        String name = pack.getDisplayName().asString();
        if (name.equals("replaymod_lang") || name.equals("replaymod_jgui")) {
            info.cancel();
        }
    }
}
//#endif
