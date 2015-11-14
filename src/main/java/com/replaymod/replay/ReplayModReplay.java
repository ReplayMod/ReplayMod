package com.replaymod.replay;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.replay.camera.*;
import com.replaymod.replay.handler.GuiHandler;
import de.johni0702.replaystudio.data.Marker;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.studio.ReplayStudio;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Mod(modid = ReplayModReplay.MOD_ID, useMetadata = true)
public class ReplayModReplay {
    public static final String MOD_ID = "replaymod-replay";

    @Mod.Instance(MOD_ID)
    public static ReplayModReplay instance;

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private final CameraControllerRegistry cameraControllerRegistry = new CameraControllerRegistry();

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
                if (replayHandler != null ) {
                    CameraEntity camera = replayHandler.getCameraEntity();
                    if (camera != null) {
                        Marker marker = new Marker();
                        marker.setTime(replayHandler.getReplaySender().currentTimeStamp());
                        marker.setX(camera.posX);
                        marker.setY(camera.posY);
                        marker.setZ(camera.posZ);
                        marker.setYaw(camera.rotationYaw);
                        marker.setPitch(camera.rotationPitch);
                        marker.setRoll(camera.roll);
                        replayHandler.getMarkers().add(marker);
                        replayHandler.saveMarkers();
                    }
                }
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

        cameraControllerRegistry.register("replaymod.camera.classic", new Function<CameraEntity, CameraController>() {
            @Nullable
            @Override
            public CameraController apply(CameraEntity cameraEntity) {
                return new ClassicCameraController(cameraEntity);
            }
        });
        cameraControllerRegistry.register("replaymod.camera.vanilla", new Function<CameraEntity, CameraController>() {
            @Nullable
            @Override
            public CameraController apply(@Nullable CameraEntity cameraEntity) {
                return new VanillaCameraController(core.getMinecraft(), cameraEntity);
            }
        });
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft mc = core.getMinecraft();
        mc.timer = new InputReplayTimer(mc.timer, this);

        new GuiHandler(this).register();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Setting.CAMERA.setChoices(new ArrayList<>(cameraControllerRegistry.getControllers()));
    }

    public void startReplay(File file) throws IOException {
        startReplay(new ZipReplayFile(new ReplayStudio(), file));
    }

    public void startReplay(ReplayFile replayFile) throws IOException {
        replayHandler = new ReplayHandler(replayFile, true);
    }

    public ReplayMod getCore() {
        return core;
    }

    public Logger getLogger() {
        return logger;
    }

    public CameraControllerRegistry getCameraControllerRegistry() {
        return cameraControllerRegistry;
    }

    public CameraController createCameraController(CameraEntity cameraEntity) {
        String controllerName = core.getSettingsRegistry().get(Setting.CAMERA);
        return cameraControllerRegistry.create(controllerName, cameraEntity);
    }
}
