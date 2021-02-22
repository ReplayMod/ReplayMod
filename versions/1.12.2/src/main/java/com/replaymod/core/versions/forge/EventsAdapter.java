package com.replaymod.core.versions.forge;

import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PostRenderWorldCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.events.PreRenderHandCallback;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.MatrixStack;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventsAdapter extends EventRegistrations {
    @SubscribeEvent
    public void onKeyEvent(InputEvent.KeyInputEvent event) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }

    @SubscribeEvent
    public void preRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        PreRenderCallback.EVENT.invoker().preRender();
    }

    @SubscribeEvent
    public void postRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PostRenderCallback.EVENT.invoker().postRender();
    }

    @SubscribeEvent
    public void renderCameraPath(RenderWorldLastEvent event) {
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(new MatrixStack());
    }

    @SubscribeEvent
    public void oRenderHand(RenderHandEvent event) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            event.setCanceled(true);
        }
    }
}
