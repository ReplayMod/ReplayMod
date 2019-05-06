package com.replaymod.replay.camera;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.mixin.EntityPlayerAccessor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

//#if MC>=11400
//$$ import net.minecraft.entity.EquipmentSlot;
//#endif

//#if MC>=11300
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
        Minecraft mc = getMinecraft();
        if (mc.gameSettings.keyBindSneak.isPressed()) {
            ReplayModReplay.instance.getReplayHandler().spectateCamera();
        }

        // Soak up all remaining key presses
        for (KeyBinding binding : Arrays.asList(mc.gameSettings.keyBindAttack, mc.gameSettings.keyBindUseItem,
                mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak, mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight)) {
            //noinspection StatementWithEmptyBody
            while (binding.isPressed());
        }

        // Prevent mouse movement
        //#if MC>=11300
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
            if (view instanceof EntityPlayer) {
                EntityPlayer viewPlayer = (EntityPlayer) view;
                //#if MC>=11400
                //$$ camera.setEquippedStack(EquipmentSlot.HEAD, viewPlayer.getEquippedStack(EquipmentSlot.HEAD));
                //#else
                camera.inventory = viewPlayer.inventory;
                //#endif
                EntityPlayerAccessor cameraA = (EntityPlayerAccessor) camera;
                EntityPlayerAccessor viewPlayerA = (EntityPlayerAccessor) viewPlayer;
                //#if MC>=10904
                cameraA.setItemStackMainHand(viewPlayerA.getItemStackMainHand());
                camera.swingingHand = viewPlayer.swingingHand;
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
