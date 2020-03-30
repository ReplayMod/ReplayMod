package com.replaymod.replay.camera;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.mixin.EntityPlayerAccessor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

//#if MC>=11400
import net.minecraft.entity.EquipmentSlot;
//#endif

//#if MC>=11400
//#else
//$$ import org.lwjgl.input.Mouse;
//#endif

import java.util.Arrays;

import static com.replaymod.core.versions.MCVer.*;

@RequiredArgsConstructor
public class SpectatorCameraController implements CameraController {
    private final CameraEntity camera;

    @Override
    public void update(float partialTicksPassed) {
        MinecraftClient mc = getMinecraft();
        if (mc.options.keySneak.wasPressed()) {
            ReplayModReplay.instance.getReplayHandler().spectateCamera();
        }

        // Soak up all remaining key presses
        for (KeyBinding binding : Arrays.asList(mc.options.keyAttack, mc.options.keyUse,
                mc.options.keyJump, mc.options.keySneak, mc.options.keyForward,
                mc.options.keyBack, mc.options.keyLeft, mc.options.keyRight)) {
            //noinspection StatementWithEmptyBody
            while (binding.wasPressed());
        }

        // Prevent mouse movement
        //#if MC>=11400
        // No longer needed
        //#else
        //$$ Mouse.updateCursor();
        //#endif

        // Always make sure the camera is in the exact same spot as the spectated entity
        // This is necessary as some rendering code for the hand doesn't respect the view entity
        // and always uses mc.thePlayer
        Entity view = getRenderViewEntity(mc);
        if (view != null && view != camera) {
            camera.setCameraPosRot(getRenderViewEntity(mc));
            // If it's a player, also 'steal' its inventory so the rendering code knows what item to render
            if (view instanceof PlayerEntity) {
                PlayerEntity viewPlayer = (PlayerEntity) view;
                //#if MC>=11400
                camera.equipStack(EquipmentSlot.HEAD, viewPlayer.getEquippedStack(EquipmentSlot.HEAD));
                camera.equipStack(EquipmentSlot.MAINHAND, viewPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
                camera.equipStack(EquipmentSlot.OFFHAND, viewPlayer.getEquippedStack(EquipmentSlot.OFFHAND));
                //#else
                //$$ camera.inventory = viewPlayer.inventory;
                //#endif
                EntityPlayerAccessor cameraA = (EntityPlayerAccessor) camera;
                EntityPlayerAccessor viewPlayerA = (EntityPlayerAccessor) viewPlayer;
                //#if MC>=10904
                cameraA.setItemStackMainHand(viewPlayerA.getItemStackMainHand());
                camera.preferredHand = viewPlayer.preferredHand;
                cameraA.setActiveItemStackUseCount(viewPlayerA.getActiveItemStackUseCount());
                //#else
                //$$ cameraA.setItemInUse(viewPlayerA.getItemInUse());
                //$$ cameraA.setItemInUseCount(viewPlayerA.getItemInUseCount());
                //#endif
            }
        }
    }

    @Override
    public void increaseSpeed() {

    }

    @Override
    public void decreaseSpeed() {

    }
}
