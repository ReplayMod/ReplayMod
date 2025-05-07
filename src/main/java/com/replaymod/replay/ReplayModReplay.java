package com.replaymod.replay;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraControllerRegistry;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.ClassicCameraController;
import com.replaymod.replay.camera.VanillaCameraController;
import com.replaymod.replay.gui.screen.GuiModCompatWarning;
import com.replaymod.replay.handler.GuiHandler;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ReplayModReplay implements Module {

    { instance = this; }
    public static ReplayModReplay instance;

    private ReplayMod core;
    public KeyBindingRegistry.Binding keyPlayPause;

    private final CameraControllerRegistry cameraControllerRegistry = new CameraControllerRegistry();

    public static Logger LOGGER = LogManager.getLogger();

    private ReplayHandler replayHandler;

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    public ReplayModReplay(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null ) {
                    CameraEntity camera = replayHandler.getCameraEntity();
                    if (camera != null) {
                        Marker marker = new Marker();
                        marker.setTime(replayHandler.getReplaySender().currentTimeStamp());
                        marker.setX(camera.getX());
                        marker.setY(camera.getY());
                        marker.setZ(camera.getZ());
                        marker.setYaw(camera.yaw);
                        marker.setPitch(camera.pitch);
                        marker.setRoll(camera.roll);
                        replayHandler.getOverlay().timeline.addMarker(marker);
                    }
                }
            }
        }, true);

        registry.registerKeyBinding("replaymod.input.thumbnail", Keyboard.KEY_N, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    MinecraftClient mc = MCVer.getMinecraft();
                    ListenableFuture<NoGuiScreenshot> future = NoGuiScreenshot.take(mc, 1280, 720);
                    Futures.addCallback(future, new FutureCallback<NoGuiScreenshot>() {
                        @Override
                        public void onSuccess(NoGuiScreenshot result) {
                            try {
                                core.printInfoToChat("replaymod.chat.savingthumb");
                                @SuppressWarnings("deprecation") // there's no easy way to produce jpg images from NativeImage
                                BufferedImage image = result.getImage().toBufferedImage();
                                // Encoding with alpha fails on OpenJDK and produces broken image on Sun JDK.
                                BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                                Graphics graphics = bgrImage.getGraphics();
                                graphics.drawImage(image, 0, 0, null);
                                graphics.dispose();
                                replayHandler.getReplayFile().writeThumb(bgrImage);
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
        }, true);

        keyPlayPause = registry.registerKeyBinding("replaymod.input.playpause", Keyboard.KEY_P, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    replayHandler.getOverlay().playPauseButton.onClick();
                }
            }
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollclockwise", Keyboard.KEY_L, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollcounterclockwise", Keyboard.KEY_J, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.resettilt", Keyboard.KEY_K, () -> {
            Optional.ofNullable(replayHandler).map(ReplayHandler::getCameraEntity).ifPresent(c -> c.roll = 0);
        }, true);

        registry.registerKeyBinding("replaymod.input.skip", Keyboard.KEY_RIGHT, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    replayHandler.relativeJumpButton(true);
                }
            }
        }, true);

        registry.registerKeyBinding("replaymod.input.rewind", Keyboard.KEY_LEFT, new Runnable() {
            @Override
            public void run() {
                if (replayHandler != null) {
                    replayHandler.relativeJumpButton(false);
                }
            }
        }, true);
    }

    @Override
    public void initClient() {
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

        new GuiHandler(this).register();
    }

    public void startReplay(File file) throws IOException {
        startReplay(core.files.open(file.toPath()));
    }

    public void startReplay(ReplayFile replayFile) throws IOException {
        startReplay(replayFile, true, true);
    }

    public ReplayHandler startReplay(ReplayFile replayFile, boolean checkModCompat, boolean asyncMode) throws IOException {
        if (replayHandler != null) {
            replayHandler.endReplay();
        }
        if (checkModCompat) {
            ModCompat.ModInfoDifference modDifference = new ModCompat.ModInfoDifference(replayFile.getModInfo());
            if (!modDifference.getMissing().isEmpty() || !modDifference.getDiffering().isEmpty()) {
                GuiModCompatWarning screen = new GuiModCompatWarning(modDifference);
                screen.loadButton.onClick(() -> {
                    try {
                        startReplay(replayFile, false, asyncMode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                screen.display();
                return null;
            }
        }
        replayHandler = new ReplayHandler(replayFile, asyncMode);
        KeyBinding.updateKeysByCode(); // see Mixin_ContextualKeyBindings

        return replayHandler;
    }

    public void forcefullyStopReplay() {
        replayHandler = null;
        KeyBinding.updateKeysByCode(); // see Mixin_ContextualKeyBindings
    }

    public ReplayMod getCore() {
        return core;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public CameraControllerRegistry getCameraControllerRegistry() {
        return cameraControllerRegistry;
    }

    public CameraController createCameraController(CameraEntity cameraEntity) {
        String controllerName = core.getSettingsRegistry().get(Setting.CAMERA);
        return cameraControllerRegistry.create(controllerName, cameraEntity);
    }
}
