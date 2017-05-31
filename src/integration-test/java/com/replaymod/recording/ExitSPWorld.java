package com.replaymod.recording;

import com.replaymod.core.AbstractTask;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.IOException;

public class ExitSPWorld extends AbstractTask {
    @Override
    protected void init() {
        mc.displayInGameMenu();
        expectGui(GuiIngameMenu.class, ingameMenu -> {
            click("Save and Quit to Title");
            expectGui(GuiMainMenu.class, mainMenu -> new Thread(() -> {
                try {
                    while (true) {
                        String[] dirs = core.getReplayFolder().list(DirectoryFileFilter.DIRECTORY);
                        if (dirs == null) {
                            future.setException(new NullPointerException("dirs is null"));
                            return;
                        }
                        if (dirs.length == 0) {
                            runLater(() -> future.set(null));
                            return;
                        }
                        Thread.sleep(10);
                    }
                } catch (IOException | InterruptedException e) {
                    future.setException(e);
                }
            }).start());
        });
    }
}
