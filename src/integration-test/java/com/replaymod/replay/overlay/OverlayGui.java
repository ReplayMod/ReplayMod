package com.replaymod.replay.overlay;

import com.replaymod.core.AbstractTask;
import com.replaymod.core.CompositeTask;
import com.replaymod.core.Task;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import org.lwjgl.input.Keyboard;

public class OverlayGui {
    public static Task whileOpened(Task...tasks) {
        Task[] nTasks = new Task[tasks.length + 2];
        System.arraycopy(tasks, 0, nTasks, 1, tasks.length);
        nTasks[0] = new Open();
        nTasks[tasks.length + 1] = new Close();
        return new CompositeTask(nTasks);
    }

    public static class Open extends AbstractTask {
        @Override
        protected void init() {
            press(Keyboard.KEY_T);
            expectGui(GuiReplayOverlay.class, done -> future.set(null));
        }
    }

    public static class Close extends AbstractTask {
        @Override
        protected void init() {
            press(Keyboard.KEY_ESCAPE);
            expectGuiClosed(() -> future.set(null));
        }
    }
}
