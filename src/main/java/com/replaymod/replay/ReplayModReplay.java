package com.replaymod.replay;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.replay.handler.GuiHandler;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.studio.ReplayStudio;
import net.minecraft.client.Minecraft;
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

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.thumbnail", Keyboard.KEY_N, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    Minecraft mc = Minecraft.getMinecraft();
                    ListenableFuture<NoGuiScreenshot> future = NoGuiScreenshot.take(mc, 1280, 720);
                    Futures.addCallback(future, new FutureCallback<NoGuiScreenshot>() {
                        @Override
                        public void onSuccess(NoGuiScreenshot result) {
                            try {
                                replayHandler.getReplayFile().writeThumb(result.getImage());
                                core.printInfoToChat("replaymod.chat.savedthumb");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                        }
                    });
                }
            }
        });

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.playpause", Keyboard.KEY_P, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    replayHandler.getOverlay().playPauseButton.onClick();
                }
            }
        });
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft mc = core.getMinecraft();
        mc.timer = new InputReplayTimer(mc.timer, this);

        new GuiHandler(this).register();
    }

    public void startReplay(File file) throws IOException {
        ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), file);
        replayHandler = new ReplayHandler(replayFile, true);
    }

    public ReplayMod getCore() {
        return core;
    }
}
