package com.replaymod.recording.mixin;

//#if MC>=10904
import com.replaymod.recording.events.GuiContainerActionEvent;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.replaymod.core.versions.MCVer.*;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @Shadow
    private Minecraft mc;

    @Inject(method = "closeScreen", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_onCloseScreen(CallbackInfo ci) {
        FORGE_BUS.post(new GuiContainerActionEvent.WindowClosed(mc.player.openContainer.windowId,mc.player.inventory.getItemStack()));
    }
}
//#endif
