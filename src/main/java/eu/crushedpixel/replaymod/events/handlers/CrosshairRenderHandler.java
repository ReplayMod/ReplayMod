package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrosshairRenderHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onCrosshairRender(RenderGameOverlayEvent.Pre event) {
        //Crosshair should only render if hovered Entity can actually be spectated
        if(ReplayHandler.isInReplay() && ReplayHandler.isCamera() && event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(!(mc.pointedEntity instanceof EntityPlayer || mc.pointedEntity instanceof EntityLiving || mc.pointedEntity instanceof EntityItemFrame));
        }
    }
}
