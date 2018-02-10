package com.replaymod.compat.oranges17animations;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Orange seems to have copied vast parts of the RendererLivingEntity into their ArmorAnimation class which cancels the RenderLivingEvent.Pre and calls its own code instead.
 * This breaks our mixin which assures that, even though the camera is in spectator mode, it cannot see invisible entities.
 *
 * To fix this issue, we simply cancel the RenderLivingEvent.Pre before it gets to ArmorAnimation if the entity is invisible.
 */
public class HideInvisibleEntities {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final boolean modLoaded = Loader.isModLoaded("animations");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void preRenderLiving(RenderLivingEvent.Pre event) {
        if (modLoaded) {
            if (player(mc) instanceof CameraEntity && getEntity(event).isInvisible()) {
                event.setCanceled(true);
            }
        }
    }
}
