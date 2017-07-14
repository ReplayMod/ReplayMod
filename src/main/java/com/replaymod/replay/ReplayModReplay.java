package com.replaymod.replay;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraControllerRegistry;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.ClassicCameraController;
import com.replaymod.replay.camera.VanillaCameraController;
import com.replaymod.replay.gui.overlay.GuiMarkerTimeline;
import com.replaymod.replay.gui.screen.GuiModCompatWarning;
import com.replaymod.replay.handler.GuiHandler;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
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
import java.util.Optional;

@Mod(modid = ReplayModReplay.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        useMetadata = true)
public class ReplayModReplay {
    public static final String MOD_ID = "replaymod-replay";

    @Mod.Instance(MOD_ID)
    public static ReplayModReplay instance;

    private ReplayMod core;

    private final CameraControllerRegistry cameraControllerRegistry = new CameraControllerRegistry();

    private Logger logger;

    protected ReplayHandler replayHandler;

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        core = ReplayMod.instance;

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

        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_DELETE, () -> {
            if (replayHandler != null) {
                GuiMarkerTimeline timeline = replayHandler.getOverlay().timeline;
                if (timeline.getSelectedMarker() != null) {
                    replayHandler.getMarkers().remove(timeline.getSelectedMarker());
                    replayHandler.saveMarkers();
                    timeline.setSelectedMarker(null);
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
                                core.printInfoToChat("replaymod.chat.savingthumb");
                                replayHandler.getReplayFile().writeThumb(result.getImage());
                                core.printInfoToChat("replaymod.chat.savedthumb");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                            core.printWarningToChat("replaymod.chat.failedthumb");
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

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollclockwise", Keyboard.KEY_L, () -> {
            // Noop, actual handling logic in CameraEntity#update
        });

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollcounterclockwise", Keyboard.KEY_J, () -> {
            // Noop, actual handling logic in CameraEntity#update
        });

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.resettilt", Keyboard.KEY_K, () -> {
            Optional.ofNullable(replayHandler).map(ReplayHandler::getCameraEntity).ifPresent(c -> c.roll = 0);
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
        startReplay(replayFile, true);
    }

    public void startReplay(ReplayFile replayFile, boolean checkModCompat) throws IOException {
        if (checkModCompat) {
            ModCompat.ModInfoDifference modDifference = new ModCompat.ModInfoDifference(replayFile.getModInfo());
            if (!modDifference.getMissing().isEmpty() || !modDifference.getDiffering().isEmpty()) {
                new GuiModCompatWarning(this, replayFile, modDifference).display();
                return;
            }
        }
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
