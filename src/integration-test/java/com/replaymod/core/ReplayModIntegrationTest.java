package com.replaymod.core;

import com.replaymod.core.regression.RegressionTest60;
import com.replaymod.core.regression.RegressionTest62;
import com.replaymod.extra.DownloadOpenEye;
import com.replaymod.online.SkipLogin;
import com.replaymod.recording.CreateSPWorld;
import com.replaymod.recording.ExitSPWorld;
import com.replaymod.replay.ExitReplay;
import com.replaymod.replay.LoadReplay;
import com.replaymod.replay.OpenReplayViewer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import static com.replaymod.core.AbstractTask.mc;
import static com.replaymod.core.ReplayModIntegrationTest.MOD_ID;
import static com.replaymod.core.Utils.addCallback;

/**
 * Helper mod that initiates the integration tests.
 */
@Mod(modid = MOD_ID)
public class ReplayModIntegrationTest {
    public static final String MOD_ID = "replaymod-integration-test";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();

        // Make sure the game window doesn't have to remain in focus during the test
        mc.gameSettings.pauseOnLostFocus = false;

        runTasks(
                new SkipLogin(),
                new DownloadOpenEye(),
                new CreateSPWorld(),
                new Wait(5000),
                new ExitSPWorld(),
                new OpenReplayViewer(),
                new LoadReplay(),

                new RegressionTest60(),
                new RegressionTest62(),

                // new AbstractTask() {}, // Uncomment to not exit on success (useful for writing more tests)
                new ExitReplay()
        );
    }

    private void runTasks(Task... tests) {
        addCallback(new CompositeTask(tests).execute(), success -> {
            if (!Minecraft.getMinecraft().hasCrashed) {
                LOGGER.info("===================================================");
                LOGGER.info("=                 ALL TESTS PASSED                =");
                LOGGER.info("===================================================");
                FMLCommonHandler.instance().exitJava(0, false);
            }
        }, error -> {
            LOGGER.error("Failed task:", error);
            LOGGER.error("===================================================");
            LOGGER.error("=                  TEST FAILED                    =");
            LOGGER.error("===================================================");
            FMLCommonHandler.instance().exitJava(1, false);
        });
    }
}
