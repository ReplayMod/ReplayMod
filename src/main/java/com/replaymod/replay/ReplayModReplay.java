package com.replaymod.replay;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.handler.GuiHandler;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.studio.ReplayStudio;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;

@Mod(modid = ReplayModReplay.MOD_ID, useMetadata = true)
public class ReplayModReplay {
    public static final String MOD_ID = "replaymod-replay";

    @Mod.Instance(MOD_ID)
    public static ReplayModReplay instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private Logger logger;

    private ReplayHandler replayHandler;

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        core.getSettingsRegistry().register(Setting.class);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        new GuiHandler(this).register();
    }

    public void startReplay(File file) throws IOException {
        ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), file);
        replayHandler = new ReplayHandler(replayFile, true);
    }
}
