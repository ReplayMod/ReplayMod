package eu.crushedpixel.replaymod.gui.overlay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.gui.GuiRenderSettings;
import eu.crushedpixel.replaymod.gui.GuiReplaySpeedSlider;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.timelines.GuiKeyframeTimeline;
import eu.crushedpixel.replaymod.gui.elements.timelines.GuiMarkerTimeline;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.Gui;
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
import java.util.List;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class GuiReplayOverlay extends Gui {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final ResourceLocation replay_gui = new ResourceLocation("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 128;
    private static final float ZOOM_STEPS = 0.05f;

    public static GuiTexturedButton texturedButton(int x, int y, int u, int v, int size, Runnable action, String hoverText) {
        return new GuiTexturedButton(0, x, y, size, size, replay_gui, u, v, TEXTURE_SIZE, TEXTURE_SIZE, action, I18n.format(hoverText));
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

    private final GuiElement buttonPlayPause = new DelegatingElement() {

        private final GuiElement buttonPlay = texturedButton(BUTTON_PLAY_PAUSE_X, TOP_ROW, 0, 0, 20, new Runnable() {
            @Override
            public void run() {
                ReplayMod.replaySender.setReplaySpeed(speedSlider.getSliderValue());
            }
        }, "replaymod.gui.ingame.menu.unpause");

        private final GuiElement buttonPause = texturedButton(BUTTON_PLAY_PAUSE_X, TOP_ROW, 0, 20, 20, new Runnable() {
            @Override
            public void run() {
                ReplayMod.replaySender.setReplaySpeed(0);
            }
        }, "replaymod.gui.ingame.menu.pause");

        @Override
        public GuiElement delegate() {
            return ReplayMod.replaySender.paused() ? buttonPlay : buttonPause;
        }
    };

    private final GuiElement buttonExport = texturedButton(BUTTON_EXPORT_X, BOTTOM_ROW, 40, 0, 20, new Runnable() {
        @Override
        public void run() {
            //if not enough keyframes, abort and leave chat message
            if(ReplayHandler.getPositionKeyframes().size() < 2 || ReplayHandler.getTimeKeyframes().size() < 1) {
                ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.morekeyframes", ChatMessageHandler.ChatMessageType.WARNING);
            } else {
                mc.displayGuiScreen(new GuiRenderSettings());
            }
        }
    }, "replaymod.gui.ingame.menu.renderpath");

    private final GuiElement buttonPlayPausePath = new DelegatingElement() {

        private final GuiElement buttonPlay = texturedButton(BUTTON_PLAY_PATH_X, BOTTOM_ROW, 0, 0, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.startPath(null);

            }
        }, "replaymod.gui.ingame.menu.playpath");

        private final GuiElement buttonPause = texturedButton(BUTTON_PLAY_PATH_X, BOTTOM_ROW, 0, 0, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.interruptReplay();

            }
        }, "replaymod.gui.ingame.menu.pausepath");

        @Override
        public GuiElement delegate() {
            return ReplayHandler.isInPath() ? buttonPause : buttonPlay;
        }
    };

    private final GuiElement buttonPlace = new DelegatingElement() {
        private final GuiElement buttonNotSelected = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 0, 40, 20, new Runnable() {
            @Override
            public void run() {
                Entity cam = mc.getRenderViewEntity();
                if (cam != null) {
                    AdvancedPosition position = new AdvancedPosition(cam.posX, cam.posY, cam.posZ, cam.rotationPitch,
                            cam.rotationYaw % 360, ReplayHandler.getCameraTilt(), null);

                    if (ReplayHandler.isCamera())
                        ReplayHandler.addKeyframe(new Keyframe<AdvancedPosition>(ReplayHandler.getRealTimelineCursor(), position));
                    else
                        ReplayHandler.addKeyframe(new Keyframe<AdvancedPosition>(ReplayHandler.getRealTimelineCursor(), new AdvancedPosition(cam.getEntityId(), true)));
                }
            }
        }, "replaymod.gui.ingame.menu.addposkeyframe");

        private final GuiElement buttonSelected = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 0, 60, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.removeKeyframe(ReplayHandler.getSelectedKeyframe());
            }
        }, "replaymod.gui.ingame.menu.removeposkeyframe");

        private final GuiElement buttonSpectatorNotSelected = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 40, 40, 20, new Runnable() {
            @Override
            public void run() {
                Entity cam = mc.getRenderViewEntity();
                if (cam != null) {
                    AdvancedPosition position = new AdvancedPosition(cam.posX, cam.posY, cam.posZ, cam.rotationPitch,
                            cam.rotationYaw % 360, ReplayHandler.getCameraTilt(), null);

                    if (ReplayHandler.isCamera())
                        ReplayHandler.addKeyframe(new Keyframe<AdvancedPosition>(ReplayHandler.getRealTimelineCursor(), position));
                    else
                        ReplayHandler.addKeyframe(new Keyframe<AdvancedPosition>(ReplayHandler.getRealTimelineCursor(), new AdvancedPosition(cam.getEntityId(), true)));
                }
            }
        }, "replaymod.gui.ingame.menu.addspeckeyframe");

        private final GuiElement buttonSpectatorSelected = texturedButton(BUTTON_PLACE_X, BOTTOM_ROW, 40, 60, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.removeKeyframe(ReplayHandler.getSelectedKeyframe());
            }
        }, "replaymod.gui.ingame.menu.removespeckeyframe");

        @Override
        public GuiElement delegate() {
            boolean selected = ReplayHandler.getSelectedKeyframe() != null && ReplayHandler.getSelectedKeyframe().getValue() instanceof AdvancedPosition;
            boolean camera;
            if(selected) {
                camera = ((Keyframe<AdvancedPosition>)ReplayHandler.getSelectedKeyframe()).getValue().getSpectatedEntityID() == null;
            } else {
                camera = ReplayHandler.isCamera();
            }

            if(camera) {
                return selected ? buttonSelected : buttonNotSelected;
            } else {
                return selected ? buttonSpectatorSelected : buttonSpectatorNotSelected;
            }
        }
    };

    private final GuiElement buttonTime = new DelegatingElement() {

        private final GuiElement buttonNotSelected = texturedButton(BUTTON_TIME_X, BOTTOM_ROW, 0, 80, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.addKeyframe(new Keyframe<TimestampValue>(ReplayHandler.getRealTimelineCursor(), new TimestampValue(ReplayMod.replaySender.currentTimeStamp())));
            }
        }, "replaymod.gui.ingame.menu.addtimekeyframe");

        private final GuiElement buttonSelected = texturedButton(BUTTON_TIME_X, BOTTOM_ROW, 0, 100, 20, new Runnable() {
            @Override
            public void run() {
                ReplayHandler.removeKeyframe(ReplayHandler.getSelectedKeyframe());
            }
        }, "replaymod.gui.ingame.menu.removetimekeyframe");

        @Override
        public GuiElement delegate() {
            return ReplayHandler.getSelectedKeyframe() != null && ReplayHandler.getSelectedKeyframe().getValue() instanceof TimestampValue ? buttonSelected : buttonNotSelected;
        }
    };
    private final GuiElement buttonZoomIn = texturedButton(WIDTH - 14 - 9, BOTTOM_ROW, 40, 20, 9, new Runnable() {
        @Override
        public void run() {
            zoom_scale = Math.max(0.025f, zoom_scale - ZOOM_STEPS);
        }
    }, "replaymod.gui.ingame.menu.zoomin");

    private final GuiElement buttonZoomOut = texturedButton(WIDTH - 14 - 9, BOTTOM_ROW + 11, 40, 30, 9, new Runnable() {

        @Override
        public void run() {
            zoom_scale = Math.min(1f, zoom_scale + ZOOM_STEPS);
            pos_left = Math.min(pos_left, 1f - zoom_scale);
        }
    }, "replaymod.gui.ingame.menu.zoomout");


    private final GuiMarkerTimeline timeline = new GuiMarkerTimeline(TIMELINE_X, TOP_ROW - 1, WIDTH - 14 - TIMELINE_X, 22, false);

    private final GuiKeyframeTimeline timelineReal = new GuiKeyframeTimeline(TIMELINE_REAL_X, BOTTOM_ROW - 1, TIMELINE_REAL_WIDTH, 22, true, true, true);
    {
        timelineReal.timelineLength = 10 * 60 * 1000;
    }

    private final GuiScrollbar scrollbar = new GuiScrollbar(TIMELINE_REAL_X, BOTTOM_ROW + 22, TIMELINE_REAL_WIDTH) {
        @Override
        public void dragged() {
            pos_left = scrollbar.sliderPosition;
        }
    };

    private final GuiReplaySpeedSlider speedSlider = new GuiReplaySpeedSlider(1, SPEED_X, TOP_ROW, I18n.format("replaymod.gui.speed"));

    private final GuiElement content = new ComposedElement(buttonPlayPause, buttonExport, buttonPlace, buttonTime,
            buttonPlayPausePath, buttonZoomIn, buttonZoomOut, timeline, timelineReal, scrollbar, speedSlider);

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
        zoom_scale = 0.1f;
        pos_left = 0;
        if (slider) {
            ReplayHandler.setRealTimelineCursor(0);
            speedSlider.reset();
        }
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
            other.speedSlider.copyValueFrom(this.speedSlider);

            this.unregister();
            other.register();
            ReplayMod.overlay = other;

            if (mc.currentScreen instanceof GuiMouseInput) {
                mc.displayGuiScreen(new GuiMouseInput(other));
            }
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

    public void mouseDrag(int mouseX, int mouseY, int button) {
        content.mouseDrag(mc, mouseX, mouseY, button);
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        content.mouseRelease(mc, mouseX, mouseY, button);
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (ReplayHandler.isInPath()) { // Only allow clicking of cancel button during path replay
            buttonPlayPausePath.mouseClick(mc, mouseX, mouseY, button);
        } else {
            content.mouseClick(mc, mouseX, mouseY, button);
        }
    }

    public void performJump(long timelineTime) {
        if (timelineTime != -1) { // Click on timeline
            //When hurrying, no Timeline jumping etc. is possible
            if(!ReplayMod.replaySender.isHurrying()) {
                if(timelineTime < ReplayMod.replaySender.currentTimeStamp()) {
                    mc.displayGuiScreen(null);
                }

                CameraEntity cam = ReplayHandler.getCameraEntity();
                if(cam != null) {
                    ReplayHandler.setLastPosition(new AdvancedPosition(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw));
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

                        // No need to remove our please-wait-screen. It'll vanish with the next
                        // render pass as it's never been a real GuiScreen in the first place.
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) throws IllegalArgumentException, IllegalAccessException {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
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

        Point mousePoint = MouseUtils.getMousePos();
        int mouseX = mousePoint.getX();
        int mouseY = mousePoint.getY();

        if (!(mc.currentScreen instanceof GuiMouseInput)) {
            // We only react to the mouse if our gui screen is opened.
            // Otherwise we just move the mouse away so it doesn't activate any buttons
            mouseX = mouseY = -1000;
        }

        // Setup scrollbar and timelines
        scrollbar.size = zoom_scale;
        scrollbar.sliderPosition = pos_left;

        timeline.cursorPosition = ReplayMod.replaySender.currentTimeStamp();
        timeline.timelineLength = ReplayMod.replaySender.replayLength();

        timelineReal.cursorPosition = ReplayHandler.getRealTimelineCursor();
        timelineReal.zoom = zoom_scale;
        timelineReal.timeStart = pos_left;

        // Draw all elements
        content.draw(mc, mouseX, mouseY);
        content.drawOverlay(mc, mouseX, mouseY);

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
     * Render the eye icon in the bottom right corner of the screen.
     * @param event Rendered post game overlay
     */
    @SubscribeEvent
    public void renderLightingIndicator(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if(ReplayMod.replaySettings.isLightingEnabled() && ReplayHandler.isInReplay()) {
            mc.renderEngine.bindTexture(replay_gui);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableAlpha();
            GlStateManager.disableLighting();
            Gui.drawModalRectWithCustomSizedTexture(WIDTH - 10 - 19, HEIGHT - 10 - 13,
                    90, 20, 19, 13, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    /**
     * Dummy interface for GUI on which this replay overlay shall not be rendered.
     */
    public interface NoOverlay {

    }
}
