package com.replaymod.replay;

import com.replaymod.core.AbstractTask;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import de.johni0702.minecraft.gui.function.Clickable;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;

public class LoadReplay extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiReplayViewer.class, replayViewer -> runLater(() -> {
            ReadableDimension size = replayViewer.getMaxSize();
            // Select first entry
            replayViewer.forEach(Clickable.class).mouseClick(new Point(size.getWidth() / 2, 40), 0);
            // Load first replay
            click(replayViewer.loadButton);

            class EventHandler {
                @SubscribeEvent
                public void onRenderIngame(RenderGameOverlayEvent.Pre event) {
                    MinecraftForge.EVENT_BUS.unregister(this);
                    runLater(() -> future.set(null));
                }
            }
            MinecraftForge.EVENT_BUS.register(new EventHandler());
        }));
    }
}
