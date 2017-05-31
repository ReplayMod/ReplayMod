package com.replaymod.core.regression;

import com.replaymod.core.AbstractTask;
import com.replaymod.core.CompositeTask;
import com.replaymod.core.Task;
import com.replaymod.replay.overlay.OverlayGui;
import com.replaymod.simplepathing.GuiPathingTasks;
import com.replaymod.simplepathing.gui.GuiEditKeyframe;

/**
 * Regression test: #61 NPE when saving in edit keyframe gui of last keyframe
 */
public class RegressionTest61 extends CompositeTask {
    public RegressionTest61() {
        super(new Task[]{
                OverlayGui.whileOpened(
                        // Place keyframe
                        Task.click(130, 50),
                        new GuiPathingTasks.ClickPositionKeyframeButton(),
                        AbstractTask.create(task -> {
                            // Double click keyframe
                            task.click(130, 50);
                            task.click(130, 50);
                            task.expectGui(GuiEditKeyframe.Position.class, gui -> {
                                task.click(gui.saveButton);
                                task.expectPopupClosed(() -> task.future.set(null));
                            });
                        }),
                        // Place second spectator keyframe
                        new GuiPathingTasks.ClickPositionKeyframeButton()
                ),
                new GuiPathingTasks.ClearKeyframeTimeline(),
        });
    }
}
