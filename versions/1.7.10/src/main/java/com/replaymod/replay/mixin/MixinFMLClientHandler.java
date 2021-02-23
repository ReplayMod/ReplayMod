package com.replaymod.replay.mixin;

import com.replaymod.replay.gui.screen.GuiOpeningReplay;
import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FMLClientHandler.class, remap = false)
public class MixinFMLClientHandler {
    // Usually this method is called async and can just block and wait for MC to tick the network manager
    // Since we sometimes call it sync though, we need to take care of ticking ourselves.
    @Inject(method = "waitForPlayClient", at = @At("HEAD"), remap = false)
    private void tickNetworkManager(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isCallingFromMinecraftThread()) {
            return;
        }
        GuiScreen gui = mc.currentScreen;
        if (gui instanceof GuiOpeningReplay) {
            gui.handleInput();
        }
    }
}
