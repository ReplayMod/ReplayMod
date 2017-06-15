package com.replaymod.extra;

import com.replaymod.core.AbstractTask;
import com.replaymod.extras.OpenEyeExtra;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.nio.file.NoSuchFileException;

public class DownloadOpenEye extends AbstractTask {
    @Override
    protected void init() {
        if ("1".equals(System.getenv("RM_INTEGRATION_TEST_NO_OPENEYE"))) {
            runLater(() -> future.set(null));
            return;
        }
        expectGui(OpenEyeExtra.OfferGui.class, offerGui -> {
            click(offerGui.yesButton);
            expectGuiClosed(20 * 1000, () -> {
                File targetFile = new File(mc.mcDataDir, "mods/" + Loader.MC_VERSION + "/OpenEye.jar");
                if (!targetFile.exists()) {
                    future.setException(new NoSuchFileException(targetFile.getAbsolutePath()));
                } else {
                    future.set(null);
                }
            });
        });
    }
}
