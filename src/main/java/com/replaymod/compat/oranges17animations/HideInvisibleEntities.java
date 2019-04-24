package com.replaymod.compat.oranges17animations;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

//#if MC>=11300
import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//#endif

import static com.replaymod.core.versions.MCVer.*;

/**
 * Orange seems to have copied vast parts of the RendererLivingEntity into their ArmorAnimation class which cancels the RenderLivingEvent.Pre and calls its own code instead.
 * This breaks our mixin which assures that, even though the camera is in spectator mode, it cannot see invisible entities.
 *
 * To fix this issue, we simply cancel the RenderLivingEvent.Pre before it gets to ArmorAnimation if the entity is invisible.
 */
public class HideInvisibleEntities {
    private final Minecraft mc = Minecraft.getInstance();
    //#if MC>=11300
    private final boolean modLoaded = ModList.get().isLoaded("animations");
    //#else
    //$$ private final boolean modLoaded = Loader.isModLoaded("animations");
    //#endif

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void preRenderLiving(RenderLivingEvent.Pre event) {
        if (modLoaded) {
            if (mc.player instanceof CameraEntity && getEntity(event).isInvisible()) {
                event.setCanceled(true);
            }
        }
    }
}
