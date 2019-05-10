package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.IGuiImage;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.options.GameOptions;

//#if MC>=11400
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.events.PostRenderCallback;
//#else
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

public class FullBrightness extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 90, 20, 19, 13).setSize(19, 13);

    private GameOptions gameSettings;
    private boolean active;
    //#if MC>=11300
    private double originalGamma;
    //#else
    //$$ private float originalGamma;
    //#endif

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.module = ReplayModReplay.instance;
        this.gameSettings = mod.getMinecraft().options;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                // need to tick once to update lightmap when replay is paused
                //#if MC>=11300
                mod.getMinecraft().gameRenderer.tick();
                //#else
                //$$ mod.getMinecraft().entityRenderer.updateRenderer();
                //#endif
                ReplayHandler replayHandler = module.getReplayHandler();
                if (replayHandler != null) {
                    updateIndicator(replayHandler.getOverlay());
                }
            }
        });

        register();
    }

    //#if MC>=11400
    { on(PreRenderCallback.EVENT, this::preRender); }
    private void preRender() {
    //#else
    //$$ @SubscribeEvent
    //$$ public void preRender(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase != TickEvent.Phase.START) return;
    //#endif
        if (active && module.getReplayHandler() != null) {
            originalGamma = gameSettings.gamma;
            gameSettings.gamma = 1000;
        }
    }

    //#if MC>=11400
    { on(PostRenderCallback.EVENT, this::postRender); }
    private void postRender() {
    //#else
    //$$ @SubscribeEvent
    //$$ public void postRender(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase != TickEvent.Phase.END) return;
    //#endif
        if (active && module.getReplayHandler() != null) {
            gameSettings.gamma = originalGamma;
        }
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay())); }
    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }
}
