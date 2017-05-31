package com.replaymod.simplepathing;

import com.replaymod.core.AbstractTask;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.simplepathing.gui.GuiPathing;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import org.lwjgl.input.Keyboard;

public abstract class GuiPathingTasks extends AbstractTask {
    @Override
    protected void init() {
        expectGui(GuiReplayOverlay.class, ign -> init0(ReplayModSimplePathing.instance.getGuiPathing()));
    }

    protected abstract void init0(GuiPathing guiPathing);

    public static class ClickPositionKeyframeButton extends GuiPathingTasks {
        @Override
        protected void init0(GuiPathing guiPathing) {
            click(guiPathing.positionKeyframeButton);
            runLater(() -> future.set(null));
        }
    }

    public static class ClearKeyframeTimeline extends AbstractTask {
        @Override
        protected void init() {
            press(Keyboard.KEY_C);
            expectGui(GuiYesNoPopup.class, popup -> {
                click(popup.getYesButton());
                expectGuiClosed(() -> future.set(null));
            });
        }
    }
}
