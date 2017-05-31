package com.replaymod.core.regression;

import com.replaymod.core.CompositeTask;
import com.replaymod.core.Task;
import com.replaymod.replay.overlay.OverlayGui;
import com.replaymod.simplepathing.GuiPathingTasks;

/**
 * Regression test: #62 Swapping the only two existing keyframes causes NPE
 */
public class RegressionTest62 extends CompositeTask {
    public RegressionTest62() {
        super(new Task[]{
                OverlayGui.whileOpened(
                        // Place first keyframe
                        Task.click(130, 50),
                        new GuiPathingTasks.ClickPositionKeyframeButton(),
                        // Place second keyframe
                        Task.click(150, 50),
                        new GuiPathingTasks.ClickPositionKeyframeButton(),
                        // Move first keyframe past second keyframe
                        Task.click(130,50),
                        Task.drag(170,50)
                ),
                new GuiPathingTasks.ClearKeyframeTimeline()
        });
    }
}
