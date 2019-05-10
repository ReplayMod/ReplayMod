//#if MC>=10800
package com.replaymod.compat.shaders;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.InvocationTargetException;

import static com.replaymod.core.versions.MCVer.getRenderPartialTicks;

//#if MC>=11400
import com.replaymod.core.events.PreRenderCallback;
//#else
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

public class ShaderBeginRender extends EventRegistrations {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     *  Invokes Shaders#beginRender when rendering a video,
     *  as this would usually get called by EntityRenderer#renderWorld,
     *  which we're not calling during rendering.
     */
    //#if MC>=11400
    { on(PreRenderCallback.EVENT, this::onRenderTickStart); }
    private void onRenderTickStart() {
    //#else
    //$$ @SubscribeEvent
    //$$ public void onRenderTickStart(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase != TickEvent.Phase.START) return;
    //#endif
        if (ShaderReflection.shaders_beginRender == null) return;
        if (ShaderReflection.config_isShaders == null) return;

        try {
            // check if video is being rendered
            if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null)
                return;

            // check if Shaders are enabled
            if (!(boolean) (ShaderReflection.config_isShaders.invoke(null))) return;

            ShaderReflection.shaders_beginRender.invoke(null, mc, getRenderPartialTicks(), 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
//#endif
