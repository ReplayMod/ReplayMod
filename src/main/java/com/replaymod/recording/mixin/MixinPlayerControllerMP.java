package com.replaymod.recording.mixin;

//#if MC>=10904
import com.replaymod.recording.events.GuiContainerActionEvent;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
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

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP implements RecordingEventHandler.RecordingEventSender {

    @Shadow
    private Minecraft mc;

    // Redirects the call to playEvent without the initial player argument to the method with that argument
    // The new method will then play it and (if applicable) record it. (See MixinWorldClient)
    // This is necessary for the block break event (particles and sound) to be recorded. Otherwise it looks like the
    // event was emitted because of a packet (player will be null) and not as it actually was (by the player).
    @Redirect(method = "onPlayerDestroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    public void replayModRecording_playEvent_fixed(World world, int type, BlockPos pos, int data) {
        world.playEvent(player(mc), type, pos, data);
    }

    @Inject(method = "windowClick", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void replayModRecording_postWindowClick(
            int windowId, int slotId, int mouseButton, ClickType type,
            EntityPlayer player, CallbackInfoReturnable<ItemStack> cir, short short1, ItemStack itemStack) {
        FORGE_BUS.post(new GuiContainerActionEvent.SlotUpdate(windowId, itemStack, slotId, mouseButton, type, short1));
    }

    @Inject(method = "func_194338_a", at=@At("HEAD"), cancellable = true)
    private void replayModReplay_onRecipeSent(int p_194338_1_, IRecipe p_194338_2_, boolean p_194338_3_, EntityPlayer p_194338_4_, CallbackInfo ci) {
        FORGE_BUS.post(new GuiContainerActionEvent.RecipeClicked(p_194338_1_, p_194338_2_, p_194338_3_, p_194338_4_));
    }
}
//#endif
