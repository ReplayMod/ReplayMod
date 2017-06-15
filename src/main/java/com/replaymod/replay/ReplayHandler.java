package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.SpectatorCameraController;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.util.Location;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.*;

import static net.minecraft.client.renderer.GlStateManager.*;
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
    private final ReplaySender replaySender;

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

    /**
     * The position at which the camera should be located after the next jump.
     */
    private Location targetCameraPosition;

    private UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        MinecraftForge.EVENT_BUS.post(new ReplayOpenEvent.Pre(this));

        markers = new ArrayList<>(replayFile.getMarkers().or(Collections.emptySet()));

        replaySender = new ReplaySender(this, replayFile, asyncMode);

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        MinecraftForge.EVENT_BUS.post(new ReplayOpenEvent.Post(this));
    }

    void restartedReplay() {
        channel.close();

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");

        MinecraftForge.EVENT_BUS.post(new ReplayCloseEvent.Pre(this));

        replaySender.terminateReplay();

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (mc.player instanceof CameraEntity) {
            mc.player.setDead();
        }

        if (mc.world != null) {
            mc.world.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }

        mc.timer.field_194149_e = WrappedTimer.DEFAULT_MS_PER_TICK;
        overlay.setVisible(false);

        ReplayModReplay.instance.replayHandler = null;

        MinecraftForge.EVENT_BUS.post(new ReplayCloseEvent.Post(this));
    }

    private void setup() {
        mc.ingameGUI.getChatGUI().clearChatMessages(true);

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

        channel = new EmbeddedChannel();
        NetworkDispatcher networkDispatcher = new NetworkDispatcher(networkManager);
        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(networkDispatcher);

        channel.pipeline().addFirst("ReplayModReplay_replaySender", replaySender);
        channel.pipeline().addLast("packet_handler", networkManager);
        channel.pipeline().fireChannelActive();
        networkDispatcher.clientToServerHandshake();
    }

    public ReplayFile getReplayFile() {
        return replayFile;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public ReplaySender getReplaySender() {
        return replaySender;
    }

    public GuiReplayOverlay getOverlay() {
        return overlay;
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
    public void spectateEntity(Entity e) {
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

        if (mc.getRenderViewEntity() != e) {
            mc.setRenderViewEntity(e);
            cameraEntity.setCameraPosRot(e);
        }
    }

    /**
     * Set the camera as the view entity.
     * This is equivalent to {@link #spectateEntity(Entity) spectateEntity(null)}.
     */
    public void spectateCamera() {
        spectateEntity(null);
    }

    /**
     * Returns whether the current view entity is the camera entity.
     * @return {@code true} if the camera is the view entity, {@code false} otherwise
     */
    public boolean isCameraView() {
        return mc.player instanceof CameraEntity && mc.player == mc.getRenderViewEntity();
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
                        drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.pleasewait"),
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

                ScaledResolution resolution = new ScaledResolution(mc);
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

                mc.getConnection().getNetworkManager().processReceivedPackets();
                for (Entity entity : mc.world.loadedEntityList) {
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
                try {
                    mc.runTick();
                } catch (IOException e) {
                    e.printStackTrace(); // This should never be thrown but whatever
                }

                //finally, updating the camera's position (which is not done by the sync jumping)
                moveCameraToTargetPosition();

                // No need to remove our please-wait-screen. It'll vanish with the next
                // render pass as it's never been a real GuiScreen in the first place.
            }
        }
    }
}
