package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.IGuiImage;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import static com.replaymod.core.versions.MCVer.*;

public class FullBrightness implements Extra {
    private ReplayModReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 90, 20, 19, 13).setSize(19, 13);

    private GameSettings gameSettings;
    private boolean active;
    private float originalGamma;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.module = ReplayModReplay.instance;
        this.gameSettings = mod.getMinecraft().gameSettings;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                mod.getMinecraft().entityRenderer.lightmapUpdateNeeded = true;
                ReplayHandler replayHandler = module.getReplayHandler();
                if (replayHandler != null) {
                    updateIndicator(replayHandler.getOverlay());
                }
            }
        });

        FML_BUS.register(this);
    }

    @SubscribeEvent
    public void preRender(TickEvent.RenderTickEvent event) {
        if (active && module.getReplayHandler() != null) {
            if (event.phase == TickEvent.Phase.START) {
                originalGamma = gameSettings.gammaSetting;
                gameSettings.gammaSetting = 1000;
            } else if (event.phase == TickEvent.Phase.END) {
                gameSettings.gammaSetting = originalGamma;
            }
        }
    }

    @SubscribeEvent
    public void replayOpened(ReplayOpenEvent.Post event) {
        updateIndicator(event.getReplayHandler().getOverlay());
    }

    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }
}
