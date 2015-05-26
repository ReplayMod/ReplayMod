package eu.crushedpixel.replaymod.gui.overlay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.gui.GuiRenderSettings;
import eu.crushedpixel.replaymod.gui.GuiReplaySpeedSlider;
import eu.crushedpixel.replaymod.gui.elements.GuiKeyframeTimeline;
import eu.crushedpixel.replaymod.gui.elements.GuiScrollbar;
import eu.crushedpixel.replaymod.gui.elements.GuiTexturedButton;
import eu.crushedpixel.replaymod.gui.elements.GuiTimeline;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.Point;

import java.io.IOException;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class GuiReplayOverlay extends Gui {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final ResourceLocation replay_gui = new ResourceLocation("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 128;
    private static final float ZOOM_STEPS = 0.05f;

    private static GuiTexturedButton texturedButton(int x, int y, int u, int v, int size) {
        return new GuiTexturedButton(0, x, y, size, size, replay_gui, u, v, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private final int displayWidth = mc.displayWidth;
    private final int displayHeight = mc.displayHeight;

    private final Point screenDimensions = MouseUtils.getScaledDimensions();
    private final int WIDTH = screenDimensions.getX();
    private final int HEIGHT = screenDimensions.getY();

    // Top row
    private final int TOP_ROW = 10;
    private final int BUTTON_PLAY_PAUSE_X = 10;
    private final int SPEED_X = 35;
    private final int SPEED_WIDTH = 100;
    private final int TIMELINE_X = SPEED_X + SPEED_WIDTH + 5;

    // Bottom row
    private final int BOTTOM_ROW = TOP_ROW + 33;
    private final int BUTTON_PLAY_PATH_X = 10;
    private final int BUTTON_EXPORT_X = BUTTON_PLAY_PATH_X + 25;
    private final int BUTTON_PLACE_X = BUTTON_EXPORT_X + 25;
    private final int BUTTON_TIME_X = BUTTON_PLACE_X + 25;
    private final int TIMELINE_REAL_X = BUTTON_TIME_X + 25;
    private final int TIMELINE_REAL_WIDTH = WIDTH - 14 - 11 - TIMELINE_REAL_X;

    private final GuiButton buttonPlay = texturedButton(BUTTON_PLAY_PAUSE_X, TOP_ROW, 0, 0, 20);
    private final GuiButton buttonPause = texturedButton(BUTTON_PLAY_PAUSE_X, TOP_ROW, 0, 20, 20);
    private final GuiButton buttonExport = texturedButton(BUTTON_EXPORT_X, BOTTOM_ROW, 40, 0, 20);
    private final GuiButton buttonPlayPath = texturedButton(BUTTON_PLAY_PATH_X, BOTTOM_ROW, 0, 0, 20);
    private final GuiButton buttonPlace = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 0, 40, 20);
    private final GuiButton buttonPlaceSelected = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 0, 60, 20);
    private final GuiButton buttonTime = texturedButton(BUTTON_TIME_X, BOTTOM_ROW, 0, 80, 20);
    private final GuiButton buttonTimeSelected = texturedButton(BUTTON_TIME_X, BOTTOM_ROW, 0, 100, 20);
    private final GuiButton buttonZoomIn = texturedButton(WIDTH - 14 - 9, BOTTOM_ROW, 40, 20, 9);
    private final GuiButton buttonZoomOut = texturedButton(WIDTH - 14 - 9, BOTTOM_ROW + 11, 40, 30, 9);

    private final GuiTimeline timeline = new GuiTimeline(TIMELINE_X, TOP_ROW - 1, WIDTH - 14 - TIMELINE_X);
    private final GuiKeyframeTimeline timelineReal = new GuiKeyframeTimeline(TIMELINE_REAL_X, BOTTOM_ROW - 1, TIMELINE_REAL_WIDTH);
    {
        timelineReal.timelineLength = 10 * 60 * 1000;
    }
    private final GuiScrollbar scrollbar = new GuiScrollbar(TIMELINE_REAL_X, BOTTOM_ROW + 22, TIMELINE_REAL_WIDTH);

    private GuiReplaySpeedSlider speedSlider = new GuiReplaySpeedSlider(1, SPEED_X, TOP_ROW, I18n.format("replaymod.gui.speed"));

    private float zoom_scale = 0.1f; //can see 1/10th of the timeline
    private double pos_left = 0f; //left border of timeline is at 0%

    public boolean isVisible() {
        return ReplayHandler.isInReplay();
    }

    /**
     * Resets the UI.
     * @param slider {@code true} if the speed-slider should be reset as well
     */
    public void resetUI(boolean slider) {
        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            mc.displayGuiScreen(null);
        }
        ReplayHandler.setRealTimelineCursor(0);
        if(slider)
            speedSlider = new GuiReplaySpeedSlider(1, SPEED_X, TOP_ROW, I18n.format("replaymod.gui.speed"));
    }

    public void register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void unregister() {
        FMLCommonHandler.instance().bus().unregister(this);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private void checkResize() {
        if (displayWidth != mc.displayWidth || displayHeight != mc.displayHeight) {
            GuiReplayOverlay other = new GuiReplayOverlay();
            other.zoom_scale = this.zoom_scale;
            other.pos_left = this.pos_left;
            other.speedSlider = this.speedSlider;

            this.unregister();
            other.register();
            ReplayMod.overlay = other;
        }
    }

    @SubscribeEvent
    public void onRenderTabList(RenderGameOverlayEvent.Pre event) { //cancelling tab list rendering and rendering help instead
        if (!isVisible()) return;
        if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            event.setCanceled(true);
        }
    }

    private void tick() {
        if (FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            Entity player = ReplayHandler.getCameraEntity();
            if(player != null) {
                player.setVelocity(0, 0, 0);
            }
        }

        checkResize();
    }

    public void mouseDrag(int mouseX, int mouseY) {
        speedSlider.mousePressed(mc, mouseX, mouseY);

        scrollbar.doDragging(mouseX);
        pos_left = scrollbar.sliderPosition;

        timelineReal.mouseDrag(mouseX, mouseY);
    }

    public void mouseReleased(int mouseX, int mouseY) {
        speedSlider.mouseReleased(mouseX, mouseY);

        scrollbar.endDragging(mouseX);
        pos_left = scrollbar.sliderPosition;

        timelineReal.mouseRelease(mouseX, mouseY);
    }

    public void mouseClicked(int mouseX, int mouseY) {
        if (buttonPlayPath.mousePressed(mc, mouseX, mouseY)) {
            if (ReplayHandler.isInPath()) {
                ReplayHandler.interruptReplay();
            } else {
                ReplayHandler.startPath(null);
            }
        }

        if (ReplayHandler.isInPath()) {
            return; // Only allow clicking of cancel button during path replay
        }

        if (ReplayMod.replaySender.paused()) {
            if (buttonPlay.mousePressed(mc, mouseX, mouseY)) {
                ReplayMod.replaySender.setReplaySpeed(speedSlider.getSliderValue());
            }
        } else {
            if (buttonPause.mousePressed(mc, mouseX, mouseY)) {
                ReplayMod.replaySender.setReplaySpeed(0);
            }
        }

        speedSlider.mousePressed(mc, mouseX, mouseY);
        scrollbar.startDragging(mouseX, mouseY);
        timelineReal.mouseClicked(mc, mouseX, mouseY);

        if (buttonExport.mousePressed(mc, mouseX, mouseY)) {
            mc.displayGuiScreen(new GuiRenderSettings());
        }

        Keyframe keyframe = ReplayHandler.getSelectedKeyframe();

        if (buttonPlace.mousePressed(mc, mouseX, mouseY)) {
            if (keyframe instanceof PositionKeyframe) {
                ReplayHandler.removeKeyframe(keyframe);
            } else {
                Entity cam = mc.getRenderViewEntity();
                if(cam != null) {
                    Position position = new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch,
                            cam.rotationYaw % 360, ReplayHandler.getCameraTilt());
                    ReplayHandler.addKeyframe(new PositionKeyframe(ReplayHandler.getRealTimelineCursor(), position));
                }
            }
        }

        if (buttonTime.mousePressed(mc, mouseX, mouseY)) {
            if (keyframe instanceof TimeKeyframe) {
                ReplayHandler.removeKeyframe(keyframe);
            } else {
                ReplayHandler.addKeyframe(new TimeKeyframe(ReplayHandler.getRealTimelineCursor(), ReplayMod.replaySender.currentTimeStamp()));
            }
        }

        if (buttonZoomIn.mousePressed(mc, mouseX, mouseY)) {
            zoom_scale = Math.max(0.025f, zoom_scale - ZOOM_STEPS);
        }

        if (buttonZoomOut.mousePressed(mc, mouseX, mouseY)) {
            zoom_scale = Math.min(1f, zoom_scale + ZOOM_STEPS);
            pos_left = Math.min(pos_left, 1f - zoom_scale);
        }

        long timelineTime = timeline.getTimeAt(mouseX, mouseY);
        if (timelineTime != -1) { // Click on timeline
            //When hurrying, no Timeline jumping etc. is possible
            if(!ReplayMod.replaySender.isHurrying()) {
                if(timelineTime < ReplayMod.replaySender.currentTimeStamp()) {
                    mc.displayGuiScreen(null);
                }

                CameraEntity cam = ReplayHandler.getCameraEntity();
                if(cam != null) {
                    ReplayHandler.setLastPosition(new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw));
                } else {
                    ReplayHandler.setLastPosition(null);
                }

                long diff = timelineTime - ReplayMod.replaySender.getDesiredTimestamp();
                if(diff != 0) {
                    if (diff > 0 && diff < 5000) { // Small difference and no time travel
                        ReplayMod.replaySender.jumpToTime((int) timelineTime);
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
                        ReplayMod.replaySender.setSyncModeAndWait();

                        // Perform the rendering using OpenGL
                        pushMatrix();
                        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                        enableTexture2D();
                        mc.getFramebuffer().bindFramebuffer(true);
                        mc.entityRenderer.setupOverlayRendering();

                        guiScreen.setWorldAndResolution(mc, WIDTH, HEIGHT);
                        guiScreen.drawScreen(0, 0, 0);

                        mc.getFramebuffer().unbindFramebuffer();
                        popMatrix();
                        pushMatrix();
                        mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
                        popMatrix();

                        Display.update();

                        // Send the packets
                        ReplayMod.replaySender.sendPacketsTill((int) timelineTime);
                        ReplayMod.replaySender.setAsyncMode(true);
                        ReplayMod.replaySender.setReplaySpeed(0);

                        // Tick twice to process all packets and position interpolation
                        try {
                            mc.runTick();
                            mc.runTick();
                        } catch (IOException e) {
                            e.printStackTrace(); // This should never be thrown but whatever
                        }

                        // No need to remove our please-wait-screen. It'll vanish with the next
                        // render pass as it's never been a real GuiScreen in the first place.
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) throws IllegalArgumentException, IllegalAccessException {
        if (!isVisible()) return;

        FMLClientHandler fml = FMLClientHandler.instance();

        tick();

        // Do not draw if GUI doesn't want us to
        if(mc.currentScreen instanceof NoOverlay) {
            return;
        }

        // If we are not currently spectating someone
        if (ReplayHandler.isCamera()) {
            ReplayGuiRegistry.hide(); // hide all normal UI
        } else {
            ReplayGuiRegistry.show(); // otherwise show them
        }

        // Replace chat and inventory GUI (opened by pressing the respective hotkey) with a dummy input GUI
        if(fml.isGUIOpen(GuiChat.class) || fml.isGUIOpen(GuiInventory.class)) {
            mc.displayGuiScreen(new GuiMouseInput(this));
        }

        GlStateManager.resetColor();

        Keyframe keyframe = ReplayHandler.getSelectedKeyframe();

        Point mousePoint = MouseUtils.getMousePos();
        int mouseX = mousePoint.getX();
        int mouseY = mousePoint.getY();

        if (!(mc.currentScreen instanceof GuiMouseInput)) {
            // We only react to the mouse if our gui screen is opened.
            // Otherwise we just move the mouse away so it doesn't activate any buttons
            mouseX = mouseY = -1000;
        }

        // Draw speed slider
        speedSlider.drawButton(mc, mouseX, mouseY);

        // Draw buttons
        buttonExport.drawButton(mc, mouseX, mouseY);

        // Draw play/pause button
        if (ReplayMod.replaySender.paused()) {
            buttonPlay.drawButton(mc, mouseX, mouseY);
        } else {
            buttonPause.drawButton(mc, mouseX, mouseY);
        }

        buttonPlayPath.drawButton(mc, mouseX, mouseY);

        // Keyframe buttons
        if (keyframe instanceof PositionKeyframe) {
            buttonPlaceSelected.drawButton(mc, mouseX, mouseY);
        } else {
            buttonPlace.drawButton(mc, mouseX, mouseY);
        }

        if (keyframe instanceof TimeKeyframe) {
            buttonTimeSelected.drawButton(mc, mouseX, mouseY);
        } else {
            buttonTime.drawButton(mc, mouseX, mouseY);
        }

        buttonZoomIn.drawButton(mc, mouseX, mouseY);
        buttonZoomOut.drawButton(mc, mouseX, mouseY);

        // Draw scrollbar for real timeline
        scrollbar.size = zoom_scale;
        scrollbar.sliderPosition = pos_left;
        scrollbar.draw(mc);

        // Finally draw timelines so that no other GUI elements overlap the mouse-position strings
        timeline.cursorPosition = ReplayMod.replaySender.currentTimeStamp();
        timeline.timelineLength = ReplayMod.replaySender.replayLength();
        timeline.draw(mc, mouseX, mouseY);

        timelineReal.cursorPosition = ReplayHandler.getRealTimelineCursor();
        timelineReal.zoom = zoom_scale;
        timelineReal.timeStart = pos_left;
        timelineReal.draw(mc, mouseX, mouseY);

        GlStateManager.enableBlend();
    }

    public void togglePlayPause() {
        if (ReplayMod.replaySender.paused()) {
            ReplayMod.replaySender.setReplaySpeed(speedSlider.getSliderValue());
        } else {
            ReplayMod.replaySender.setReplaySpeed(0);
        }
    }

    /**
     * Dummy interface for GUI on which this replay overlay shall not be rendered.
     */
    public static interface NoOverlay {

    }
}
