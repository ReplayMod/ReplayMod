package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.SpectatorCameraController;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.util.Location;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.*;

//#if MC<10904
//$$ import de.johni0702.minecraft.gui.element.GuiLabel;
//$$ import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
//$$ import de.johni0702.minecraft.gui.utils.Colors;
//#endif

//#if MC>=10800
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.EnumPacketDirection;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

import static net.minecraft.client.renderer.GlStateManager.*;
//#else
//$$ import cpw.mods.fml.common.Loader;
//$$ import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
//$$ import com.replaymod.replay.gui.screen.GuiOpeningReplay;
//$$ import io.netty.channel.ChannelOutboundHandlerAdapter;
//$$ import net.minecraft.client.network.NetHandlerLoginClient;
//$$ import net.minecraft.entity.EntityLivingBase;
//$$ import net.minecraft.network.EnumConnectionState;
//$$
//$$ import java.net.InetSocketAddress;
//$$ import java.net.SocketAddress;
//$$
//$$ import static com.replaymod.core.versions.MCVer.GlStateManager.*;
//#endif

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ReplayHandler {

    private static Minecraft mc = Minecraft.getMinecraft();

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

    private final List<Marker> markers;

    private final GuiReplayOverlay overlay;

    private EmbeddedChannel channel;

    private int replayDuration;

    /**
     * The position at which the camera should be located after the next jump.
     */
    private Location targetCameraPosition;

    private UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        replayDuration = replayFile.getMetaData().getDuration();

        FML_BUS.post(new ReplayOpenEvent.Pre(this));

        markers = new ArrayList<>(replayFile.getMarkers().or(Collections.emptySet()));

        fullReplaySender = new FullReplaySender(this, replayFile, false);
        //#if MC>=10904
        quickReplaySender = new QuickReplaySender(ReplayModReplay.instance, replayFile);
        //#endif

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        FML_BUS.post(new ReplayOpenEvent.Post(this));

        fullReplaySender.setAsyncMode(asyncMode);
    }

    void restartedReplay() {
        channel.close();

        // Force re-creation of camera entity by unloading the previous world
        mc.addScheduledTask(() -> {
            mc.setIngameNotInFocus();
            mc.loadWorld(null);
        });

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");

        FML_BUS.post(new ReplayCloseEvent.Pre(this));

        fullReplaySender.terminateReplay();
        //#if MC>=10904
        quickReplaySender.unregister();
        //#endif

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (player(mc) instanceof CameraEntity) {
            player(mc).setDead();
        }

        if (world(mc) != null) {
            world(mc).sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }

        //#if MC>=11200
        mc.timer.tickLength = WrappedTimer.DEFAULT_MS_PER_TICK;
        //#else
        //$$ mc.timer.timerSpeed = 1;
        //#endif
        overlay.setVisible(false);

        ReplayModReplay.instance.replayHandler = null;

        mc.displayGuiScreen(null);

        FML_BUS.post(new ReplayCloseEvent.Post(this));
    }

    private void setup() {
        //#if MC>=11100
        mc.ingameGUI.getChatGUI().clearChatMessages(false);
        //#else
        //$$ mc.ingameGUI.getChatGUI().clearChatMessages();
        //#endif

        //#if MC>=10800
        NetworkManager networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };
        NetHandlerPlayClient netHandlerPlayClient =
                new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
        networkManager.setNetHandler(netHandlerPlayClient);
        FMLClientHandler.instance().setPlayClient(netHandlerPlayClient);

        //#if MC>=11200
        channel = new EmbeddedChannel();
        NetworkDispatcher networkDispatcher = new NetworkDispatcher(networkManager);
        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(networkDispatcher);

        channel.pipeline().addFirst("ReplayModReplay_replaySender", fullReplaySender);
        channel.pipeline().addFirst("ReplayModReplay_quickReplaySender", quickReplaySender);
        channel.pipeline().addLast("packet_handler", networkManager);
        channel.pipeline().fireChannelActive();
        networkDispatcher.clientToServerHandshake();
        //#else
        //$$ channel = new EmbeddedChannel(networkManager);
        //$$ NetworkDispatcher networkDispatcher = new NetworkDispatcher(networkManager);
        //$$ channel.attr(NetworkDispatcher.FML_DISPATCHER).set(networkDispatcher);
        //$$
        //$$ channel.pipeline().addFirst("ReplayModReplay_replaySender", fullReplaySender);
        //#if MC>=10904
        //$$ channel.pipeline().addFirst("ReplayModReplay_quickReplaySender", quickReplaySender);
        //#endif
        //$$ channel.pipeline().addAfter("ReplayModReplay_replaySender", "fml:packet_handler", networkDispatcher);
        //$$ channel.pipeline().fireChannelActive();
        //#endif
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
        //$$ networkManager.setNetHandler(new NetHandlerLoginClient(networkManager, mc, null));
        //$$
        //$$ mc.displayGuiScreen(new GuiOpeningReplay(networkManager));
        //$$
        //$$ ChannelOutboundHandlerAdapter dummyHandler = new ChannelOutboundHandlerAdapter();
        //$$ channel = new EmbeddedChannel(dummyHandler);
        //$$ channel.pipeline().remove(dummyHandler);
        //$$ channel.pipeline().addFirst("ReplayModReplay_replaySender", fullReplaySender);
        //$$ channel.pipeline().addAfter("ReplayModReplay_replaySender", "packet_handler", networkManager);
        //$$ channel.pipeline().fireChannelActive();
        //$$
        //$$ // Call twice to force-overwrite the NetworkManager's internal state
        //$$ networkManager.setConnectionState(EnumConnectionState.PLAY);
        //$$ networkManager.getNetHandler().onConnectionStateTransition(EnumConnectionState.LOGIN, EnumConnectionState.PLAY);
        //$$ networkManager.setConnectionState(EnumConnectionState.PLAY);
        //$$
        //$$ FMLNetworkHandler.fmlClientHandshake(networkManager);
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
                    Utils.error(LOGGER, overlay, CrashReport.makeCrashReport(t, message), popup::close);
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
        if (quickMode == this.quickMode) return;
        if (quickMode && fullReplaySender.isAsyncMode()) {
            // If this method is called via runLater, then it cannot switch to sync mode by itself as there might be
            // some rogue packets in the task queue after it. Instead the caller must switch to sync mode first and
            // use runLater until all packets have been processed (when using setAsyncModeAndWait, one runLater should
            // be sufficient).
            throw new IllegalStateException("Cannot switch to quick mode while in async mode.");
        }
        this.quickMode = quickMode;
        if (quickMode) {
            quickReplaySender.register();
            quickReplaySender.restart();
            quickReplaySender.sendPacketsTill(fullReplaySender.currentTimeStamp());
        } else {
            quickReplaySender.unregister();
            fullReplaySender.sendPacketsTill(0);
            fullReplaySender.sendPacketsTill(quickReplaySender.currentTimeStamp());
        }
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
     * Returns all markers.
     * When changed, {@link #saveMarkers()} should be called to save the changes.
     * @return Collection of markers in no particular order
     */
    public Collection<Marker> getMarkers() {
        return markers;
    }

    /**
     * Saves all markers.
     */
    public void saveMarkers() {
        try {
            replayFile.writeMarkers(new HashSet<>(markers));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } else if (e instanceof EntityPlayer) {
            spectating = e.getUniqueID();
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
        return player(mc) instanceof CameraEntity && player(mc) == getRenderViewEntity(mc);
    }

    /**
     * Returns the camera entity.
     * @return The camera entity or {@code null} if it does not yet exist
     */
    public CameraEntity getCameraEntity() {
        return player(mc) instanceof CameraEntity ? (CameraEntity) player(mc) : null;
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
            quickReplaySender.sendPacketsTill(targetTime);
            return;
        }
        //#endif
        FullReplaySender replaySender = fullReplaySender;

        if (replaySender.isHurrying()) {
            return; // When hurrying, no Timeline jumping etc. is possible
        }

        if (targetTime < replaySender.currentTimeStamp()) {
            mc.displayGuiScreen(null);
        }

        if (retainCameraPosition) {
            CameraEntity cam = getCameraEntity();
            if (cam != null) {
                targetCameraPosition = new Location(cam.posX, cam.posY, cam.posZ,
                        cam.rotationYaw, cam.rotationPitch);
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
                GuiScreen guiScreen = new GuiScreen() {
                    @Override
                    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
                        drawBackground(0);
                        drawCenteredString(getFontRenderer(mc), I18n.format("replaymod.gui.pleasewait"),
                                width / 2, height / 2, 0xffffffff);
                    }
                };

                // Make sure that the replaysender changes into sync mode
                replaySender.setSyncModeAndWait();

                // Perform the rendering using OpenGL
                pushMatrix();
                clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                enableTexture2D();
                mc.getFramebuffer().bindFramebuffer(true);
                mc.entityRenderer.setupOverlayRendering();

                ScaledResolution resolution = newScaledResolution(mc);
                guiScreen.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
                guiScreen.drawScreen(0, 0, 0);

                mc.getFramebuffer().unbindFramebuffer();
                popMatrix();
                pushMatrix();
                mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
                popMatrix();

                Display.update();

                // Send the packets
                replaySender.sendPacketsTill(targetTime);
                replaySender.setAsyncMode(true);
                replaySender.setReplaySpeed(0);

                getConnection(mc).getNetworkManager().processReceivedPackets();
                for (Entity entity : loadedEntityList(world(mc))) {
                    if (entity instanceof EntityOtherPlayerMP) {
                        EntityOtherPlayerMP e = (EntityOtherPlayerMP) entity;
                        e.setPosition(e.otherPlayerMPX, e.otherPlayerMPY, e.otherPlayerMPZ);
                        e.rotationYaw = (float) e.otherPlayerMPYaw;
                        e.rotationPitch = (float) e.otherPlayerMPPitch;
                    }
                    entity.lastTickPosX = entity.prevPosX = entity.posX;
                    entity.lastTickPosY = entity.prevPosY = entity.posY;
                    entity.lastTickPosZ = entity.prevPosZ = entity.posZ;
                    entity.prevRotationYaw = entity.rotationYaw;
                    entity.prevRotationPitch = entity.rotationPitch;
                }
                //#if MC>=10800
                try {
                    mc.runTick();
                } catch (IOException e) {
                    e.printStackTrace(); // This should never be thrown but whatever
                }
                //#else
                //$$ mc.runTick();
                //#endif

                //finally, updating the camera's position (which is not done by the sync jumping)
                moveCameraToTargetPosition();

                // No need to remove our please-wait-screen. It'll vanish with the next
                // render pass as it's never been a real GuiScreen in the first place.
            }
        }
    }
}
