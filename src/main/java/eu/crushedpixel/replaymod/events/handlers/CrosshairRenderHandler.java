package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrosshairRenderHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void preCrosshairRender(RenderGameOverlayEvent.Pre event) {
        //Crosshair should only render if hovered Entity can actually be spectated
        if(ReplayHandler.isInReplay() && ReplayHandler.isCamera() && event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            boolean cancel = !SpectatingHandler.canSpectate(mc.pointedEntity);
            event.setCanceled(cancel);
        }
    }

    @SubscribeEvent
    public void preChatRender(RenderGameOverlayEvent.Pre event) {
        if(ReplayHandler.isInReplay() && ReplayHandler.isCamera() && event.type == RenderGameOverlayEvent.ElementType.CHAT) {
            //when a crosshair was displayed, the background of the lowest line of chat would be opaque
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }
}
