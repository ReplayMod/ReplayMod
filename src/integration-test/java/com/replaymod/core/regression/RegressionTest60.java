package com.replaymod.core.regression;

import com.replaymod.core.AbstractTask;
import com.replaymod.core.CompositeTask;
import com.replaymod.core.Task;
import com.replaymod.replay.SpectatePlayer;
import com.replaymod.replay.overlay.OverlayGui;
import com.replaymod.simplepathing.GuiPathingTasks;
import com.replaymod.simplepathing.gui.GuiEditKeyframe;
import org.lwjgl.input.Keyboard;

import static com.replaymod.core.AbstractTask.mc;
import static com.replaymod.core.Utils.times;

/**
 * Regression test: #60 Crash in path preview when two spectator keyframes are closer than 50ms
 */
public class RegressionTest60 extends CompositeTask {
    public RegressionTest60() {
        super(new Task[]{
                new SpectatePlayer(),
                OverlayGui.whileOpened(
                        // Place first spectator keyframe
                        new GuiPathingTasks.ClickPositionKeyframeButton(),
                        // Place second spectator keyframe
                        Task.click(130, 50),
                        new GuiPathingTasks.ClickPositionKeyframeButton(),
                        // Move second keyframe to 20ms on the keyframe timeline
                        AbstractTask.create(task -> {
                            // Double click keyframe
                            task.click(130, 50);
                            task.click(130, 50);
                            task.expectGui(GuiEditKeyframe.Spectator.class, gui -> {
                                // Set ms field to 20
                                task.click(mc.currentScreen.width / 2 + 80, mc.currentScreen.height / 2);
                                times(4, () -> task.press(Keyboard.KEY_BACK));
                                task.type("20");
                                // Set other fields to 0
                                times(2, () -> {
                                    task.press(Keyboard.KEY_TAB);
                                    times(4, () -> task.press(Keyboard.KEY_BACK));
                                    task.press(Keyboard.KEY_0);
                                });
                                task.click(gui.saveButton);
                                task.expectPopupClosed(() -> task.future.set(null));
                            });
                        }),
                        // Place second spectator keyframe
                        new GuiPathingTasks.ClickPositionKeyframeButton()
                ),
                // Stop spectating player
                new SpectatePlayer.End(),
                // Enable path preview
                Task.pressKey(Keyboard.KEY_H),
                new GuiPathingTasks.ClearKeyframeTimeline(),
                Task.pressKey(Keyboard.KEY_H),
        });
    }
}
