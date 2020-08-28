package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.SpectatorCameraController;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayClosingCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.util.Location;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;

import java.io.IOException;
import java.util.*;

//#if MC>=11600
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11500
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
//#endif

//#if MC>=11400
import com.replaymod.replay.mixin.EntityLivingBaseAccessor;
import net.minecraft.client.util.Window;
import net.minecraft.entity.LivingEntity;
//#else
//$$ import com.replaymod.replay.mixin.EntityOtherPlayerMPAccessor;
//$$ import net.minecraft.client.entity.EntityOtherPlayerMP;
//$$ import net.minecraft.client.gui.ScaledResolution;
//$$ import org.lwjgl.opengl.Display;
//#endif

//#if MC>=11200
//#else
//$$ import io.netty.channel.ChannelOutboundHandlerAdapter;
//#endif

//#if MC<10904
//$$ import de.johni0702.minecraft.gui.element.GuiLabel;
//$$ import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
//$$ import de.johni0702.minecraft.gui.utils.Colors;
//#endif

//#if MC>=10800
import net.minecraft.network.NetworkSide;
//#if MC>=11400
//#else
//#if MC>=11400
//$$ import net.minecraftforge.fml.network.NetworkHooks;
//#else
//$$ import com.mojang.authlib.GameProfile;
//$$ import net.minecraft.client.network.NetHandlerPlayClient;
//$$ import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
//#endif
//#endif
//#else
//$$ import cpw.mods.fml.client.FMLClientHandler;
//$$ import cpw.mods.fml.common.Loader;
//$$ import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
//$$ import com.replaymod.replay.gui.screen.GuiOpeningReplay;
//$$ import net.minecraft.entity.EntityLivingBase;
//$$
//$$ import java.net.InetSocketAddress;
//$$ import java.net.SocketAddress;
//#endif

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;
import static com.mojang.blaze3d.platform.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ReplayHandler {

    private static MinecraftClient mc = getMinecraft();

    /**
     * The file currently being played.
     */
    private final ReplayFile replayFile;

    /**
     * Decodes and sends packets into channel.
     */
    private final FullReplaySender fullReplaySender;
    //#if MC>=10904
    private final QuickReplaySender quickReplaySender;
    private boolean quickMode = false;
    //#else
    //$$ private static final String QUICK_MODE_MIN_MC = "1.9.4";
    //#endif

    /**
     * Currently active replay restrictions.
     */
    private Restrictions restrictions = new Restrictions();

    /**
     * Whether camera movements by user input and/or server packets should be suppressed.
     */
    private boolean suppressCameraMovements;

    private Set<Marker> markers;

    private final GuiReplayOverlay overlay;

    private EmbeddedChannel channel;

    private int replayDuration;

    /**
     * The position at which the camera should be located after the next jump.
     */
    private Location targetCameraPosition;

    private UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(isOnMainThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        replayDuration = replayFile.getMetaData().getDuration();

        markers = replayFile.getMarkers().or(Collections.emptySet());

        fullReplaySender = new FullReplaySender(this, replayFile, false);
        //#if MC>=10904
        quickReplaySender = new QuickReplaySender(ReplayModReplay.instance, replayFile);
        //#endif

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        ReplayOpenedCallback.EVENT.invoker().replayOpened(this);

        fullReplaySender.setAsyncMode(asyncMode);
    }

    void restartedReplay() {
        Preconditions.checkState(isOnMainThread(), "Must be called from Minecraft thread.");

        channel.close();

        //#if MC>=11400
        mc.mouse.unlockCursor();
        //#else
        //$$ mc.setIngameNotInFocus();
        //#endif

        // Force re-creation of camera entity by unloading the previous world
        //#if MC>=11400
        mc.disconnect();
        //#else
        //$$ // We need to re-set the GUI screen because having one with `allowsUserInput = true` active during world
        //$$ // load (i.e. before player is set) will crash MC...
        //$$ mc.displayGuiScreen(new net.minecraft.client.gui.GuiScreen() {});
        //$$ mc.loadWorld(null);
        //#endif

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(isOnMainThread(), "Must be called from Minecraft thread.");

        ReplayClosingCallback.EVENT.invoker().replayClosing(this);

        fullReplaySender.terminateReplay();
        //#if MC>=10904
        if (quickMode) {
            quickReplaySender.unregister();
        }
        //#endif

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (mc.player instanceof CameraEntity) {
            //#if MC>=11400
            mc.player.remove();
            //#else
            //$$ mc.player.setDead();
            //#endif
        }

        if (mc.world != null) {
            //#if MC>=11400
            mc.disconnect();
            //#else
            //$$ mc.world.sendQuittingDisconnectingPacket();
            //$$ mc.loadWorld(null);
            //#endif
        }

        TimerAccessor timer = (TimerAccessor) ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11200
        timer.setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
        //#else
        //$$ timer.setTimerSpeed(1);
        //#endif
        overlay.setVisible(false);

        ReplayModReplay.instance.forcefullyStopReplay();

        mc.openScreen(null);

        ReplayClosedCallback.EVENT.invoker().replayClosed(this);
    }

    private void setup() {
        Preconditions.checkState(isOnMainThread(), "Must be called from Minecraft thread.");

        //#if MC>=11100
        mc.inGameHud.getChatHud().clear(false);
        //#else
        //$$ mc.ingameGUI.getChatGUI().clearChatMessages();
        //#endif

        //#if MC>=10800
        ClientConnection networkManager = new ClientConnection(NetworkSide.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };
        //#else
        //$$ NetworkManager networkManager = new NetworkManager(true) {
        //$$     @Override
        //$$     public SocketAddress getRemoteAddress() {
        //$$         // See https://github.com/Dyonovan/TCNodeTracker/issues/37
        //$$         if (Loader.isModLoaded("tcnodetracker")) {
        //$$             StackTraceElement elem = Thread.currentThread().getStackTrace()[2];
        //$$             if ("com.dyonovan.tcnodetracker.events.ClientConnectionEvent".equals(elem.getClassName())) {
        //$$                 LOGGER.debug("TCNodeTracker crash workaround applied");
        //$$                 return new InetSocketAddress("replaymod.dummy", 0);
        //$$             }
        //$$         }
        //$$         return super.getRemoteAddress();
        //$$     }
        //$$
        //$$     @Override
        //$$     public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
        //$$         t.printStackTrace();
        //$$     }
        //$$ };
        //$$ mc.displayGuiScreen(new GuiOpeningReplay(networkManager));
        //$$ FMLClientHandler.instance().connectToRealmsServer(null, 0); // just to init the playClientBlock latch
        //#endif

        networkManager.setPacketListener(new ClientLoginNetworkHandler(
                networkManager,
                mc,
                null
                //#if MC>=11400
                , it -> {}
                //#endif
        ));


        //#if MC>=11200
        channel = new EmbeddedChannel();
        //#else
        //$$ ChannelOutboundHandlerAdapter dummyHandler = new ChannelOutboundHandlerAdapter();
        //$$ channel = new EmbeddedChannel(dummyHandler);
        //$$ channel.pipeline().remove(dummyHandler);
        //#endif
        //#if MC>=10904
        channel.pipeline().addLast("ReplayModReplay_quickReplaySender", quickReplaySender);
        //#endif
        channel.pipeline().addLast("ReplayModReplay_replaySender", fullReplaySender);
        channel.pipeline().addLast("packet_handler", networkManager);
        channel.pipeline().fireChannelActive();

        //#if MC>=11400 && MC<11400
        //$$ NetworkHooks.registerClientLoginChannel(networkManager);
        //#endif
    }

    public ReplayFile getReplayFile() {
        return replayFile;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public ReplaySender getReplaySender() {
        //#if MC>=10904
        return quickMode ? quickReplaySender : fullReplaySender;
        //#else
        //$$ return fullReplaySender;
        //#endif
    }

    public GuiReplayOverlay getOverlay() {
        return overlay;
    }

    //#if MC>=10904
    public void ensureQuickModeInitialized(Runnable andThen) {
        if (Utils.ifMinimalModeDoPopup(overlay, () -> {})) return;
        ListenableFuture<Void> future = quickReplaySender.getInitializationPromise();
        if (future == null) {
            InitializingQuickModePopup popup = new InitializingQuickModePopup(overlay);
            future = quickReplaySender.initialize(progress -> popup.progressBar.setProgress(progress.floatValue()));
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    String message = "Failed to initialize quick mode. It will not be available.";
                    Utils.error(LOGGER, overlay, CrashReport.create(t, message), popup::close);
                }
            });
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                andThen.run();
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                // Exception already printed in callback added above
            }
        });
    }

    private class InitializingQuickModePopup extends AbstractGuiPopup<InitializingQuickModePopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar(popup).setSize(300, 20)
                .setI18nLabel("replaymod.gui.loadquickmode");

        public InitializingQuickModePopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected InitializingQuickModePopup getThis() {
            return this;
        }
    }

    public void setQuickMode(boolean quickMode) {
        if (ReplayMod.isMinimalMode()) {
            throw new UnsupportedOperationException("Quick Mode not supported in minimal mode.");
        }
        if (quickMode == this.quickMode) return;
        if (quickMode && fullReplaySender.isAsyncMode()) {
            // If this method is called via runLater, then it cannot switch to sync mode by itself as there might be
            // some rogue packets in the task queue after it. Instead the caller must switch to sync mode first and
            // use runLater until all packets have been processed (when using setAsyncModeAndWait, one runLater should
            // be sufficient).
            throw new IllegalStateException("Cannot switch to quick mode while in async mode.");
        }
        this.quickMode = quickMode;

        CameraEntity cam = getCameraEntity();
        if (cam != null) {
            targetCameraPosition = new Location(Entity_getX(cam), Entity_getY(cam), Entity_getZ(cam), cam.yaw, cam.pitch);
        } else {
            targetCameraPosition = null;
        }

        if (quickMode) {
            quickReplaySender.register();
            quickReplaySender.restart();
            quickReplaySender.sendPacketsTill(fullReplaySender.currentTimeStamp());
        } else {
            quickReplaySender.unregister();
            fullReplaySender.sendPacketsTill(0);
            fullReplaySender.sendPacketsTill(quickReplaySender.currentTimeStamp());
        }

        moveCameraToTargetPosition();
    }

    public boolean isQuickMode() {
        return quickMode;
    }
    //#else
    //$$ public void ensureQuickModeInitialized(@SuppressWarnings("unused") Runnable andThen) {
    //$$     GuiInfoPopup.open(overlay,
    //$$             new GuiLabel().setI18nText("replaymod.gui.noquickmode", QUICK_MODE_MIN_MC).setColor(Colors.BLACK));
    //$$ }
    //$$
    //$$ public void setQuickMode(@SuppressWarnings("unused") boolean quickMode) {
    //$$     throw new UnsupportedOperationException("Quick Mode not supported on this version.");
    //$$ }
    //$$
    //$$ public boolean isQuickMode() {
    //$$     return false;
    //$$ }
    //#endif

    public int getReplayDuration() {
        return replayDuration;
    }

    /**
     * Return whether camera movement by user inputs and/or server packets should be suppressed.
     * @return {@code true} if these kinds of movement should be suppressed
     */
    public boolean shouldSuppressCameraMovements() {
        return suppressCameraMovements;
    }

    /**
     * Set whether camera movement by user inputs and/or server packets should be suppressed.
     * @param suppressCameraMovements {@code true} to suppress these kinds of movement, {@code false} to allow them
     */
    public void setSuppressCameraMovements(boolean suppressCameraMovements) {
        this.suppressCameraMovements = suppressCameraMovements;
    }

    /**
     * Spectate the specified entity.
     * When the entity is {@code null} or the camera entity, the camera becomes the view entity.
     * @param e The entity to spectate
     */
    //#if MC>=10800
    public void spectateEntity(Entity e) {
    //#else
    //$$ public void spectateEntity(EntityLivingBase e) {
    //#endif
        CameraEntity cameraEntity = getCameraEntity();
        if (cameraEntity == null) {
            return; // Cannot spectate if we have no camera
        }
        if (e == null || e == cameraEntity) {
            spectating = null;
            e = cameraEntity;
        } else if (e instanceof PlayerEntity) {
            spectating = e.getUuid();
        }

        if (e == cameraEntity) {
            cameraEntity.setCameraController(ReplayModReplay.instance.createCameraController(cameraEntity));
        } else {
            cameraEntity.setCameraController(new SpectatorCameraController(cameraEntity));
        }

        if (getRenderViewEntity(mc) != e) {
            setRenderViewEntity(mc, e);
            cameraEntity.setCameraPosRot(e);
        }
    }

    /**
     * Set the camera as the view entity.
     * This is equivalent to {@code spectateEntity(null)}.
     */
    public void spectateCamera() {
        spectateEntity(null);
    }

    /**
     * Returns whether the current view entity is the camera entity.
     * @return {@code true} if the camera is the view entity, {@code false} otherwise
     */
    public boolean isCameraView() {
        return mc.player instanceof CameraEntity && mc.player == getRenderViewEntity(mc);
    }

    /**
     * Returns the camera entity.
     * @return The camera entity or {@code null} if it does not yet exist
     */
    public CameraEntity getCameraEntity() {
        return mc.player instanceof CameraEntity ? (CameraEntity) mc.player : null;
    }

    public UUID getSpectatedUUID() {
        return spectating;
    }

    public void setTargetPosition(Location pos) {
        targetCameraPosition = pos;
    }

    public void moveCameraToTargetPosition() {
        CameraEntity cam = getCameraEntity();
        if (cam != null && targetCameraPosition != null) {
            cam.setCameraPosRot(targetCameraPosition);
        }
    }

    public void doJump(int targetTime, boolean retainCameraPosition) {
        //#if MC>=10904
        if (getReplaySender() == quickReplaySender) {
            // Always round to full tick
            targetTime = targetTime + targetTime % 50;

            if (targetTime >= 50) {
                // Jump to time of previous tick first
                quickReplaySender.sendPacketsTill(targetTime - 50);
            }

            // Update all entity positions (especially prev/lastTick values)
            for (Entity entity : loadedEntityList(mc.world)) {
                skipTeleportInterpolation(entity);
                entity.lastRenderX = entity.prevX = Entity_getX(entity);
                entity.lastRenderY = entity.prevY = Entity_getY(entity);
                entity.lastRenderZ = entity.prevZ = Entity_getZ(entity);
                entity.prevYaw = entity.yaw;
                entity.prevPitch = entity.pitch;
            }

            // Run previous tick
            //#if MC>=11400
            mc.tick();
            //#else
            //$$ try {
            //$$     mc.runTick();
            //$$ } catch (IOException e) {
            //$$     throw new RuntimeException(e);
            //$$ }
            //#endif

            // Jump to target tick
            quickReplaySender.sendPacketsTill(targetTime);

            // Immediately apply player teleport interpolation
            for (Entity entity : loadedEntityList(mc.world)) {
                skipTeleportInterpolation(entity);
            }
            return;
        }
        //#endif
        FullReplaySender replaySender = fullReplaySender;

        if (replaySender.isHurrying()) {
            return; // When hurrying, no Timeline jumping etc. is possible
        }

        if (targetTime < replaySender.currentTimeStamp()) {
            mc.openScreen(null);
        }

        if (retainCameraPosition) {
            CameraEntity cam = getCameraEntity();
            if (cam != null) {
                targetCameraPosition = new Location(Entity_getX(cam), Entity_getY(cam), Entity_getZ(cam),
                        cam.yaw, cam.pitch);
            } else {
                targetCameraPosition = null;
            }
        }

        long diff = targetTime - replaySender.getDesiredTimestamp();
        if (diff != 0) {
            if (diff > 0 && diff < 5000) { // Small difference and no time travel
                replaySender.jumpToTime(targetTime);
            } else { // We either have to restart the replay or send a significant amount of packets
                // Render our please-wait-screen
                GuiScreen guiScreen = new GuiScreen();
                guiScreen.setBackground(AbstractGuiScreen.Background.DIRT);
                guiScreen.addElements(new HorizontalLayout.Data(0.5),
                        new GuiLabel().setI18nText("replaymod.gui.pleasewait"));

                // Make sure that the replaysender changes into sync mode
                replaySender.setSyncModeAndWait();

                // Perform the rendering using OpenGL
                pushMatrix();
                clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                        //#if MC>=11400
                        , true
                        //#endif
                );
                enableTexture();
                mc.getFramebuffer().beginWrite(true);
                //#if MC>=11500
                Window window = getWindow(mc);
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                RenderSystem.matrixMode(GL11.GL_PROJECTION);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0, window.getFramebufferWidth() / window.getScaleFactor(), window.getFramebufferHeight() / window.getScaleFactor(), 0, 1000, 3000);
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0, 0, -2000);
                //#else
                //#if MC>=11400
                //$$ getWindow(mc).method_4493(true);
                //#else
                //#if MC>=11400
                //$$ mc.mainWindow.setupOverlayRendering();
                //#else
                //$$ mc.entityRenderer.setupOverlayRendering();
                //#endif
                //#endif
                //#endif

                //#if MC>=11400
                Window
                //#else
                //$$ ScaledResolution
                //#endif
                        resolution = newScaledResolution(mc);
                guiScreen.toMinecraft().init(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
                //#if MC>=11600
                //$$ guiScreen.toMinecraft().render(new MatrixStack(), 0, 0, 0);
                //#else
                //#if MC>=11400
                guiScreen.toMinecraft().render(0, 0, 0);
                //#else
                //$$ guiScreen.toMinecraft().drawScreen(0, 0, 0);
                //#endif
                //#endif
                guiScreen.toMinecraft().removed();

                mc.getFramebuffer().endWrite();
                popMatrix();
                pushMatrix();
                //#if MC>=11400
                mc.getFramebuffer().draw(getWindow(mc).getFramebufferWidth(), getWindow(mc).getFramebufferHeight());
                //#else
                //$$ mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
                //#endif
                popMatrix();

                //#if MC>=11500
                getWindow(mc).swapBuffers();
                //#else
                //#if MC>=11400
                //$$ getWindow(mc).setFullscreen(true);
                //#else
                //$$ Display.update();
                //#endif
                //#endif

                // Send the packets
                replaySender.sendPacketsTill(targetTime);
                replaySender.setAsyncMode(true);
                replaySender.setReplaySpeed(0);

                //#if MC<10800
                //$$ while (mc.currentScreen instanceof GuiOpeningReplay) {
                //$$     mc.currentScreen.handleInput();
                //$$ }
                //#endif

                mc.getNetworkHandler().getConnection()
                        //#if MC>=11400
                        .tick();
                        //#else
                        //$$ .processReceivedPackets();
                        //#endif
                for (Entity entity : loadedEntityList(mc.world)) {
                    skipTeleportInterpolation(entity);
                    entity.lastRenderX = entity.prevX = Entity_getX(entity);
                    entity.lastRenderY = entity.prevY = Entity_getY(entity);
                    entity.lastRenderZ = entity.prevZ = Entity_getZ(entity);
                    entity.prevYaw = entity.yaw;
                    entity.prevPitch = entity.pitch;
                }
                //#if MC>=10800 && MC<11400
                //$$ try {
                //$$     mc.runTick();
                //$$ } catch (IOException e) {
                //$$     e.printStackTrace(); // This should never be thrown but whatever
                //$$ }
                //#else
                mc.tick();
                //#endif

                //finally, updating the camera's position (which is not done by the sync jumping)
                moveCameraToTargetPosition();

                // No need to remove our please-wait-screen. It'll vanish with the next
                // render pass as it's never been a real GuiScreen in the first place.
            }
        }
    }

    private void skipTeleportInterpolation(Entity entity) {
        //#if MC>=11400
        if (entity instanceof LivingEntity && !(entity instanceof CameraEntity)) {
            LivingEntity e = (LivingEntity) entity;
            EntityLivingBaseAccessor ea = (EntityLivingBaseAccessor) e;
            e.updatePosition(ea.getInterpTargetX(), ea.getInterpTargetY(), ea.getInterpTargetZ());
            e.yaw = (float) ea.getInterpTargetYaw();
            e.pitch = (float) ea.getInterpTargetPitch();
        }
        //#else
        //$$ if (entity instanceof EntityOtherPlayerMP) {
        //$$     EntityOtherPlayerMP e = (EntityOtherPlayerMP) entity;
        //$$     EntityOtherPlayerMPAccessor ea = (EntityOtherPlayerMPAccessor) e;
        //$$     e.setPosition(ea.getOtherPlayerMPX(), ea.getOtherPlayerMPY(), ea.getOtherPlayerMPZ());
        //$$     e.rotationYaw = (float) ea.getOtherPlayerMPYaw();
        //$$     e.rotationPitch = (float) ea.getOtherPlayerMPPitch();
        //$$ }
        //#endif
    }
}
