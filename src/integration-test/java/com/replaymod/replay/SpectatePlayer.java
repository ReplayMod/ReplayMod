package com.replaymod.replay;

import com.replaymod.core.AbstractTask;
import com.replaymod.extras.playeroverview.PlayerOverviewGui;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.TimeoutException;

public class SpectatePlayer extends AbstractTask {
    @Override
    protected void init() {
        press(Keyboard.KEY_B);
        expectGui(PlayerOverviewGui.class, overview -> {
            click(overview.getMaxSize().getWidth() / 2 - 60, 60);
            press(Keyboard.KEY_ESCAPE);
            expectGuiClosed(() -> future.set(null));
        });
    }

    public static class End extends AbstractTask {
        private int timeout;

        @Override
        protected void init() {
            runLater(() -> press(Keyboard.KEY_LSHIFT));
        }

        @SubscribeEvent
        public void onTick(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            if (timeout++ > 20) {
                future.setException(new TimeoutException("Camera hasn't stopped spectating."));
                return;
            }
            if (mc.getRenderViewEntity() == mc.player) {
                future.set(null);
            }
        }
    }
}
