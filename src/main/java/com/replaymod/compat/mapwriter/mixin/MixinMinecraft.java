//#if MC<11400
//$$ package com.replaymod.compat.mapwriter.mixin;
//$$
//$$ import com.replaymod.replay.ReplayModReplay;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.multiplayer.ServerData;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.Loader;
//#else
//$$ import cpw.mods.fml.common.Loader;
//#endif
//$$
//$$ /**
//$$  * Approximately this for <1.12: https://github.com/Vectron/mapwriter/commit/68234520c7a3a0ae8201a085d7e66369900586ac
//$$  */
//$$ @Mixin(Minecraft.class)
//$$ public abstract class MixinMinecraft {
//$$
//$$     @Shadow
//$$     private ServerData currentServerData;
//$$
//$$     @Inject(method = "getCurrentServerData", cancellable = true, at = @At("HEAD"))
//$$     private void replayModCompat_fixBug96(CallbackInfoReturnable<ServerData> ci) {
//$$         if (currentServerData == null
//$$                 && (Loader.isModLoaded("mapwriter") || Loader.isModLoaded("MapWriter"))
//$$                 && ReplayModReplay.instance.getReplayHandler() != null) {
//$$             for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
//$$                 if ("mapwriter.util.Utils".equals(elem.getClassName()) && "getWorldName".equals(elem.getMethodName())) {
                    //#if MC>=10809
                    //$$ ci.setReturnValue(new ServerData(null, "replay", false));
                    //#else
                    //$$ ci.setReturnValue(new ServerData(null, "replay"));
                    //#endif
//$$                     return;
//$$                 }
//$$             }
//$$         }
//$$     }
//$$ }
//#endif
