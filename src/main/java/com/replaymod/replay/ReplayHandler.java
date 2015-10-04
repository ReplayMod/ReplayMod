package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.replaystudio.data.Marker;
import de.johni0702.replaystudio.replay.ReplayFile;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import com.replaymod.core.utils.Restrictions;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.Point;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    private final Set<Marker> markers;

    private final GuiReplayOverlay overlay;

    private EmbeddedChannel channel;

    /**
     * The position at which the camera should be located after the next jump.
     */
    private AdvancedPosition targetCameraPosition;

    protected UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        FMLCommonHandler.instance().bus().post(new ReplayOpenEvent.Pre(this));

        markers = replayFile.getMarkers().or(Collections.<Marker>emptySet());

        replaySender = new ReplaySender(this, replayFile, asyncMode);

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        FMLCommonHandler.instance().bus().post(new ReplayOpenEvent.Post(this));
    }

    void restartedReplay() {
        channel.close();

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(mc.isCallingFromMinecraftThread(), "Must be called from Minecraft thread.");

        FMLCommonHandler.instance().bus().post(new ReplayCloseEvent.Pre(this));

        replaySender.terminateReplay();

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (mc.theWorld != null) {
            mc.theWorld.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
        }

        overlay.setVisible(false);

        // These might have been hidden by the overlay, so we have to make sure they're visible again
        ReplayGuiRegistry.show();

        FMLCommonHandler.instance().bus().post(new ReplayCloseEvent.Post(this));
    }

    private void setup() {
        mc.ingameGUI.getChatGUI().clearChatMessages();

        NetworkManager networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };
        networkManager.setNetHandler(new NetHandlerPlayClient(
                mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player")));

        channel = new EmbeddedChannel(networkManager);
        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));

        channel.pipeline().addFirst(replaySender);
        channel.pipeline().fireChannelActive();
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
     * @return Set of markers
     */
    public Set<Marker> getMarkers() {
        return markers;
    }

    /**
     * Saves all markers.
     */
    public void saveMarkers() {
        try {
            replayFile.writeMarkers(markers);
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
        if (e == null || e == cameraEntity) {
            spectating = null;
            e = cameraEntity;
        } else if (e instanceof EntityPlayer) {
            spectating = e.getUniqueID();
        }

        if (mc.getRenderViewEntity() != e) {
            mc.setRenderViewEntity(e);
            cameraEntity.updatePos(e);
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
        return mc.thePlayer instanceof CameraEntity && mc.thePlayer == mc.getRenderViewEntity();
    }

    /**
     * Returns the camera entity.
     * @return The camera entity or {@code null} if it does not yet exist
     */
    public CameraEntity getCameraEntity() {
        return mc.thePlayer instanceof CameraEntity ? (CameraEntity) mc.thePlayer : null;
    }

    public void setTargetPosition(AdvancedPosition pos) {
        targetCameraPosition = pos;
    }

    public void moveCameraToTargetPosition() {
        CameraEntity cam = getCameraEntity();
        if (cam != null && targetCameraPosition != null) {
            cam.moveAbsolute(targetCameraPosition);
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
                targetCameraPosition = new AdvancedPosition(cam.posX, cam.posY, cam.posZ,
                        cam.rotationPitch, cam.rotationYaw, cam.roll);
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

                Point point = MouseUtils.getScaledDimensions();
                guiScreen.setWorldAndResolution(mc, point.getX(), point.getY());
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

                mc.getNetHandler().getNetworkManager().processReceivedPackets();
                @SuppressWarnings("unchecked")
                List<Entity> entities = (List<Entity>) mc.theWorld.loadedEntityList;
                for (Entity entity : entities) {
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
