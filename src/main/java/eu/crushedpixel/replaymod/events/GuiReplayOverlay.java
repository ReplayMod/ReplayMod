package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.*;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.video.VideoWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class GuiReplayOverlay extends Gui {

    private final Minecraft mc = Minecraft.getMinecraft();
    int sl_begin_x = 0;
    int sl_end_x = 63;
    int sl_y = 40;
    int plus_x = 0;
    int plus_y = 0;
    int minus_x = 0;
    int minus_y = 9;
    int slider_begin_x = 1;
    int slider_begin_width = 1;
    int slider_end_x = 62;
    int slider_end_width = 1;
    int slider_y = 50;
    int slider_height = 7;
    private int sliderX = 35;
    private int sliderY = 10;
    private int timelineX = sliderX + 100 + 5;
    private int realTimelineX = 10 + 4 * 25;
    private int realTimelineY = 33 + 10;
    private int ppButtonX = 10;
    private int ppButtonY = 10;
    private int r_ppButtonX = 10;
    private int r_ppButtonY = realTimelineY + 1;
    private int exportButtonX = 35;
    private int exportButtonY = realTimelineY + 1;
    private int place_ButtonX = 60;
    private int place_ButtonY = realTimelineY + 1;
    private int time_ButtonX = 85;
    private int time_ButtonY = realTimelineY + 1;
    private ResourceLocation replay_gui = new ResourceLocation("replaymod", "replay_gui.png");
    private ResourceLocation extended_gui = new ResourceLocation("replaymod", "extended_gui.png");
    private ResourceLocation timeline_icons = new ResourceLocation("replaymod", "timeline_icons.png");
    private GuiReplaySpeedSlider speedSlider;
    private boolean mouseDown = false;
    private int tl_begin_x = 0;
    private int tl_begin_width = 4;
    private int tl_end_x = 60;
    private int tl_end_width = 4;
    private int tl_middle_x = 4;
    private int tl_y = 40;
    private float zoom_scale = 0.1f; //can see 1/10th of the timeline
    private float pos_left = 0f; //left border of timeline is at 0%
    private long timelineLength = 10 * 60 * 1000; //10 min of timeline
    private float zoom_steps = 0.05f;
    private boolean wasSliding = false;
    private boolean mouseDwn = false;

    public void resetUI(boolean slider) throws Exception {
        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            mc.displayGuiScreen(null);
        }
        ReplayHandler.setRealTimelineCursor(0);
        if(slider)
            speedSlider = new GuiReplaySpeedSlider(1, sliderX, sliderY, I18n.format("replaymod.gui.speed"));
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent event) {
        if(ReplayProcess.isVideoRecording() && ReplayHandler.isInPath() && !(mc.currentScreen instanceof GuiCancelRender)) {
            if(event.isCancelable()) event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public void onRenderTabList(RenderGameOverlayEvent.Pre event) { //cancelling tab list rendering and rendering help instead
        if(ReplayHandler.isInReplay() && event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void renderRecordingIndicator(RenderGameOverlayEvent.Text event) {
        if(!ReplayHandler.isInReplay() && ReplayMod.replaySettings.showRecordingIndicator() && ConnectionEventHandler.isRecording()) {
            this.drawString(mc.fontRendererObj, I18n.format("replaymod.gui.recording").toUpperCase(), 30, 18 - (mc.fontRendererObj.FONT_HEIGHT / 2), Color.WHITE.getRGB());
            mc.renderEngine.bindTexture(replay_gui);
            GlStateManager.resetColor();
            GlStateManager.enableAlpha();
            this.drawModalRectWithCustomSizedTexture(10, 10, 40, 21, 16, 16, 64, 64);
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) throws IllegalArgumentException, IllegalAccessException {

        if(!ReplayHandler.isInReplay() || FMLClientHandler.instance().isGUIOpen(GuiSpectateSelection.class)
                || VideoWriter.isRecording() || FMLClientHandler.instance().isGUIOpen(GuiKeyframeRepository.class)) {
            return;
        }

        if(!ReplayGuiRegistry.hidden) ReplayGuiRegistry.hide();

        if(FMLClientHandler.instance().isGUIOpen(GuiChat.class) || FMLClientHandler.instance().isGUIOpen(GuiInventory.class)) {
            mc.displayGuiScreen(new GuiMouseInput());
        }

        GL11.glEnable(GL11.GL_BLEND);

        Point mousePoint = MouseUtils.getMousePos();
        final int mouseX = (int) mousePoint.getX();
        final int mouseY = (int) mousePoint.getY();

        Point scaled = MouseUtils.getScaledDimensions();
        final int width = (int) scaled.getX();
        final int height = (int) scaled.getY();

        //Draw Timeline
        drawTimeline(timelineX, width - 14, 9);
        drawRealTimeline(realTimelineX, width - 14 - 11, realTimelineY, mouseX, mouseY);

        //Play/Pause button
        int x = 0;
        int y = 0;

        boolean play = !ReplayMod.replaySender.paused();
        boolean hover = false;

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= ppButtonX && mouseX <= ppButtonX + 20
                    && mouseY >= ppButtonY && mouseY <= ppButtonY + 20) {
                hover = true;
            }
        }

        if(play) {
            y = 20;
        }
        if(hover) {
            x = 20;
        }

        mc.renderEngine.bindTexture(replay_gui);

        GlStateManager.resetColor();
        this.drawModalRectWithCustomSizedTexture(ppButtonX, ppButtonY, x, y, 20, 20, 64, 64);

        //When hurrying, no Timeline jumping etc. is possible
        if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) { //clicking the Button
            if(!ReplayMod.replaySender.isHurrying()) {
                speedSlider.mousePressed(mc, mouseX, mouseY);
                if(!mouseDown) {
                    mouseDown = true;
                    if(hover) {
                        playOrPause();
                    } else if(mouseX >= exportButtonX && mouseX <= exportButtonX + 20 && mouseY >= exportButtonY && exportButtonY <= exportButtonY + 20) {
                        ReplayHandler.startPath(true);
                    }

                    if(mouseX >= timelineX + 4 && mouseX <= width - 18 && mouseY >= 11 && mouseY <= 29) {
                        double tot = (width - 18) - (timelineX + 4);
                        double perc = (mouseX - (timelineX + 4)) / tot;
                        double time = perc * (double) ReplayMod.replaySender.replayLength();

                        if(time < ReplayMod.replaySender.currentTimeStamp()) {
                            mc.displayGuiScreen(null);
                        }

                        CameraEntity cam = ReplayHandler.getCameraEntity();
                        if(cam != null) {
                            ReplayHandler.setLastPosition(new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw));
                        } else {
                            ReplayHandler.setLastPosition(null);
                        }

                        if((int) time != ReplayMod.replaySender.getDesiredTimestamp())
                            ReplayMod.replaySender.jumpToTime((int) time);
                    }
                }
            }

        } else {
            try {
                speedSlider.mouseReleased(mouseX, mouseY);
                mouseDown = false;
            } catch(Exception e) {
            }
        }

        hover = false;
        x = 0;
        y = 18;

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= exportButtonX && mouseX <= exportButtonX + 20
                    && mouseY >= exportButtonY && mouseY <= exportButtonY + 20) {
                hover = true;
            }
        }

        if(hover) {
            x = 20;
        }

        mc.renderEngine.bindTexture(timeline_icons);

        GlStateManager.resetColor();
        this.drawModalRectWithCustomSizedTexture(exportButtonX, exportButtonY, x, y, 20, 20, 64, 64);

        //GlStateManager.resetColor();

        //Place Keyframe Button
        hover = false;
        x = 0;
        y = 0;

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= place_ButtonX && mouseX <= place_ButtonX + 20
                    && mouseY >= place_ButtonY && mouseY <= place_ButtonY + 20) {
                hover = true;
            }
        }

        if(hover) {
            x = 20;
        }

        if(ReplayHandler.getSelected() != null && ReplayHandler.getSelected() instanceof PositionKeyframe) {
            y += 20;
        }

        mc.renderEngine.bindTexture(extended_gui);

        if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)
                && !ReplayHandler.isInPath()) {
            if(ReplayHandler.getSelected() == null || !(ReplayHandler.getSelected() instanceof PositionKeyframe)) {
                addPlaceKeyframe();
            } else {
                ReplayHandler.removeKeyframe(ReplayHandler.getSelected());
            }
        }

        GlStateManager.resetColor();
        this.drawModalRectWithCustomSizedTexture(place_ButtonX, place_ButtonY, x, y, 20, 20, 64, 64);


        //Time Keyframe Button
        hover = false;
        x = 0;
        y = 40;

        boolean timeSelected = false;
        if(ReplayHandler.getSelected() != null && ReplayHandler.getSelected() instanceof TimeKeyframe) {
            timeSelected = true;
            x = 40;
            y = 0;
        }

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= time_ButtonX && mouseX <= time_ButtonX + 20
                    && mouseY >= time_ButtonY && mouseY <= time_ButtonY + 20) {
                hover = true;
            }
        }

        if(hover) {
            if(timeSelected) {
                y = 20;
            } else {
                x = 20;
            }
        }

        mc.renderEngine.bindTexture(extended_gui);

        if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(ReplayHandler.getSelected() == null || !(ReplayHandler.getSelected() instanceof TimeKeyframe) && !ReplayHandler.isInPath()) {
                addTimeKeyframe();
            } else {
                ReplayHandler.removeKeyframe(ReplayHandler.getSelected());
            }
        }

        GlStateManager.resetColor();
        this.drawModalRectWithCustomSizedTexture(time_ButtonX, time_ButtonY, x, y, 20, 20, 64, 64);

        if(mouseX >= (timelineX + 4) && mouseX <= width - 18 && mouseY >= 11 && mouseY <= 29) {
            double tot = (width - 18) - (timelineX + 4);
            double perc = (mouseX - (timelineX + 4)) / tot;
            long time = Math.round(perc * (double) ReplayMod.replaySender.replayLength());

            String timestamp = (String.format("%02d:%02ds",
                    TimeUnit.MILLISECONDS.toMinutes(time),
                    TimeUnit.MILLISECONDS.toSeconds(time) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
            ));

            this.drawCenteredString(mc.fontRendererObj, timestamp, mouseX, mouseY + 5, Color.WHITE.getRGB());
        }

        if(mc.inGameHasFocus) {
            Mouse.setCursorPosition(width / 2, height / 2);
        }

        try {
            speedSlider.drawButton(mc, mouseX, mouseY);
        } catch(Exception e) {
        }

        GlStateManager.resetColor();

        Entity player = ReplayHandler.getCameraEntity();
        if(player != null) {
            player.setVelocity(0, 0, 0);
        }

        if(!Mouse.isButtonDown(0)) isClick();
    }

    private void drawTimeline(int minX, int maxX, int y) {
        int zero = minX + tl_begin_width;
        int full = maxX - tl_end_width;

        GlStateManager.resetColor();
        mc.renderEngine.bindTexture(replay_gui);
        this.drawModalRectWithCustomSizedTexture(minX, y, tl_begin_x, tl_y, tl_begin_width, 22, 64, 64);

        for(int i = minX + tl_begin_width; i < maxX - tl_end_width; i += tl_end_x - tl_begin_width) {
            this.drawModalRectWithCustomSizedTexture(i, y, tl_begin_x + tl_begin_width
                    , tl_y, Math.min(tl_end_x - tl_begin_width, maxX - tl_end_width - i)
                    , 22, 64, 64);
        }

        this.drawModalRectWithCustomSizedTexture(maxX - tl_end_width, y, tl_end_x, tl_y, tl_end_width, 22, 64, 64);

        //Cursor
        double width = full - zero;
        double perc = (double) ReplayMod.replaySender.currentTimeStamp() / (double) ReplayMod.replaySender.replayLength();

        int cursorX = (int) Math.round(zero + (perc * width));
        this.drawModalRectWithCustomSizedTexture(cursorX - 3, y + 3, 44, 0, 8, 16, 64, 64);
    }

    private void drawRealTimeline(int minX, int maxX, int y, int mouseX, int mouseY) {
        int zero = minX + tl_begin_width;
        int full = maxX - tl_end_width;

        //the real timeline
        GlStateManager.resetColor();
        mc.renderEngine.bindTexture(replay_gui);
        this.drawModalRectWithCustomSizedTexture(minX, y, tl_begin_x, tl_y, tl_begin_width, 22, 64, 64);

        for(int i = minX + tl_begin_width; i < maxX - tl_end_width; i += tl_end_x - tl_begin_width) {
            this.drawModalRectWithCustomSizedTexture(i, y, tl_begin_x + tl_begin_width
                    , tl_y, Math.min(tl_end_x - tl_begin_width, maxX - tl_end_width - i)
                    , 22, 64, 64);
        }

        this.drawModalRectWithCustomSizedTexture(maxX - tl_end_width, y, tl_end_x, tl_y, tl_end_width, 22, 64, 64);

        //Time Slider
        int yo = y + 22 + 1;
        GlStateManager.resetColor();
        mc.renderEngine.bindTexture(timeline_icons);
        this.drawModalRectWithCustomSizedTexture(minX, yo, sl_begin_x, sl_y, 2, 9, 64, 64);

        for(int i = minX + 2; i < maxX - 1; i += sl_end_x - 2) {
            this.drawModalRectWithCustomSizedTexture(i, yo, 2, sl_y,
                    Math.min(sl_end_x - 2, maxX - 1 - i), 9, 64, 64);
        }

        this.drawModalRectWithCustomSizedTexture(maxX - 1, yo, sl_end_x, sl_y, 1, 9, 64, 64);

        //Timeline Pos Slider
        int sl_y = yo + 1;
        int minPos = minX + 1;
        int maxPos = maxX - 2;
        int tlWidth = maxPos - minPos;

        int slider_min = minPos + Math.round(pos_left * tlWidth);
        int slider_width = Math.round(zoom_scale * tlWidth);

        int sl_max = slider_min + slider_width;

        this.drawModalRectWithCustomSizedTexture(slider_min, sl_y, slider_begin_x, slider_y, slider_begin_width, slider_height, 64, 64);

        for(int i = slider_min + slider_begin_width; i < sl_max - slider_end_width; i += slider_end_x - slider_begin_width - slider_begin_x) {
            this.drawModalRectWithCustomSizedTexture(i, sl_y, slider_begin_x + slider_begin_width, slider_y,
                    Math.min(slider_end_x - slider_end_width - slider_begin_x, sl_max - slider_end_width - i), slider_height, 64, 64);
        }

        this.drawModalRectWithCustomSizedTexture(sl_max - slider_end_width, sl_y, slider_end_x, slider_y,
                slider_end_width, slider_height, 64, 64);

        //Slider dragging
        if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class) && (mouseX >= slider_min && mouseX <= sl_max && mouseY >= sl_y && mouseY <= sl_y + slider_height || wasSliding)) {
            wasSliding = true;
            float dx = ((float) Mouse.getDX() * (float) new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth() / mc.displayWidth);
            this.pos_left = Math.min(1f - this.zoom_scale, Math.max(0f, this.pos_left + (dx / (float) tlWidth)));
        }

        if(!Mouse.isButtonDown(0)) {
            wasSliding = false;
        }

        //Timeline Buttons
        //+- Buttons
        boolean hover = false;
        int px = plus_x;
        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= maxX + 2 && mouseX <= maxX + 2 + 9
                    && mouseY >= y + 1 && mouseY <= y + 1 + 9) {
                hover = true;
            }
        }

        if(hover) {
            px += 9;
        }

        this.drawModalRectWithCustomSizedTexture(maxX + 2, y + 1, px, plus_y, 9, 9, 64, 64);

        if(hover && Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            zoomIn();
        }

        hover = false;
        int mx = minus_x;

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= maxX + 2 && mouseX <= maxX + 2 + 9
                    && mouseY >= y + 9 + 3 && mouseY <= y + 9 + 3 + 9) {
                hover = true;
            }
        }

        if(hover) {
            mx += 9;
        }

        this.drawModalRectWithCustomSizedTexture(maxX + 2, y + 9 + 3, mx, minus_y, 9, 9, 64, 64);

        if(hover && Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            zoomOut();
        }

        //show Time String
        if(mouseX >= zero && mouseX <= full && mouseY >= y && mouseY <= y + 22) {
            long tot = Math.round((double) timelineLength * zoom_scale);
            double perc = (mouseX - (realTimelineX + 4)) / (double) (full - zero);

            long time = Math.round(this.pos_left * (double) timelineLength) + Math.round(perc * (double) tot);

            String timestamp = (String.format("%02d:%02ds",
                    TimeUnit.MILLISECONDS.toMinutes(time),
                    TimeUnit.MILLISECONDS.toSeconds(time) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
            ));
            this.drawCenteredString(mc.fontRendererObj, timestamp, mouseX, mouseY + 5, Color.WHITE.getRGB());
        }

        //draw Markers on timeline
        MarkerType mt = MarkerType.getMarkerType(zoom_scale, timelineLength);

        //every x seconds, draw small marker
        long left_real = Math.round(pos_left * (double) timelineLength);
        long right_real = left_real + (Math.round(zoom_scale * timelineLength));
        long tot = Math.round((double) timelineLength * zoom_scale);

        for(int s = 0; s <= timelineLength; s += mt.getSmallDistance()) {
            if(s > right_real) break;
            if(s >= left_real) {
                //calculate absolute position on screen
                long relative = (s) - (left_real);
                double perc = ((double) relative / (double) tot);

                long real_width = full - zero;
                long rel_x = Math.round(perc * (double) real_width);

                long real_x = zero + rel_x;

                this.drawVerticalLine((int) real_x, y + 19 - 3, y + 19, Color.WHITE.getRGB());
            }
        }

        //every x seconds, draw big marker
        for(int s = 0; s <= timelineLength; s += mt.getDistance()) {
            if(s > right_real) break;
            if(s >= left_real) {
                //calculate absolute position on screen
                long relative = s - (left_real);
                double perc = ((double) relative / (double) tot);

                long real_width = full - zero;
                long rel_x = Math.round(perc * (double) real_width);

                long real_x = zero + rel_x;

                this.drawVerticalLine((int) real_x, y + 19 - 7, y + 19, Color.LIGHT_GRAY.getRGB());

                //write text
                int time = s;
                String timestamp = (String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(time) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                ));

                this.drawCenteredString(mc.fontRendererObj, timestamp, (int) real_x, y - 8, Color.WHITE.getRGB());
            }
        }

        //handle Mouse clicks on realTimeLine
        if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class) && !wasSliding && mouseX >= minX + tl_begin_width && mouseX <= maxX - tl_end_width &&
                mouseY >= y && mouseY <= y + 22) {

            //calculate real time and set cursor accordingly
            int width = (maxX - tl_end_width) - (minX + tl_begin_width);
            int rel_x = mouseX - (minX + tl_begin_width);

            float rel_pos = (float) rel_x / (float) width;

            float abs_width = (zoom_scale * (float) timelineLength);
            int real_pos = Math.round(left_real + ((rel_pos) * abs_width));

            ReplayHandler.setRealTimelineCursor(real_pos);

            //Keyframe click handling here
            if(isClick()) {
                //tolerance is 2 pixels multiplied with the timespan of one pixel
                int tolerance = 2 * Math.round(abs_width / (float) width);

                if(mouseY >= y + 9) {
                    TimeKeyframe close = ReplayHandler.getClosestTimeKeyframeForRealTime(ReplayHandler.getRealTimelineCursor(), tolerance);
                    ReplayHandler.selectKeyframe(close); //can be null, deselects keyframe
                } else {
                    PositionKeyframe close = ReplayHandler.getClosestPlaceKeyframeForRealTime(ReplayHandler.getRealTimelineCursor(), tolerance);
                    ReplayHandler.selectKeyframe(close); //can be null, deselects keyframe
                }
            }
        }

        //Draw Realtime Cursor
        if(ReplayHandler.getRealTimelineCursor() >= left_real && ReplayHandler.getRealTimelineCursor() <= right_real) {
            long rel_pos = ReplayHandler.getRealTimelineCursor() - left_real;
            long rel_width = right_real - left_real;
            double perc = (double) rel_pos / (double) rel_width;

            int real_width = (maxX - tl_end_width) - (minX + tl_begin_width);
            double rel_x = (float) real_width * perc;

            int real_x = (int) Math.round((minX + tl_begin_width) + rel_x);
            mc.renderEngine.bindTexture(this.replay_gui);

            GL11.glEnable(GL11.GL_BLEND);
            this.drawModalRectWithCustomSizedTexture(real_x - 3, y + 3, 44, 0, 8, 16, 64, 64);
            //this.drawModalRectWithCustomSizedTexture(real_x, sl_y, u, v, width, height, textureWidth, textureHeight)
        }


        //Draw Keyframe logos
        mc.renderEngine.bindTexture(timeline_icons);
        for(Keyframe kf : ReplayHandler.getKeyframes()) {
            if(kf.getRealTimestamp() > right_real) break;
            if(kf.getRealTimestamp() >= left_real) {
                int dx = 18;
                int dy = 0;

                int ry = y + 3;

                if(kf instanceof TimeKeyframe) {
                    dy = 5;
                    ry += 5;
                }
                if(ReplayHandler.isSelected(kf)) {
                    dx += 5;
                }

                long relative = kf.getRealTimestamp() - (left_real);
                double perc = ((double) relative / (double) tot);

                long real_width = full - zero;
                long rel_x = Math.round(perc * (double) real_width);

                long real_x = zero + rel_x - 2;

                this.drawModalRectWithCustomSizedTexture((int) real_x, ry, dx, dy, 5, 5, 64, 64);
            }
        }

        //Draw Play/Pause Button
        //Play/Pause button

        int dx = 0;
        int dy = 0;

        boolean play = ReplayHandler.isInPath();
        hover = false;

        if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(mouseX >= r_ppButtonX && mouseX <= r_ppButtonX + 20
                    && mouseY >= r_ppButtonY && mouseY <= r_ppButtonY + 20) {
                hover = true;
            }
        }

        if(play) {
            dy = 20;
        }
        if(hover) {
            dx = 20;
        }

        mc.renderEngine.bindTexture(replay_gui);

        GlStateManager.resetColor();
        this.drawModalRectWithCustomSizedTexture(r_ppButtonX, r_ppButtonY, dx, dy, 20, 20, 64, 64);

        //Handling the click on the Replay starter
        if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
            if(ReplayHandler.isInPath()) {
                ReplayHandler.interruptReplay();
            } else {
                ReplayHandler.startPath(false);
            }
        }

    }

    private void addPlaceKeyframe() {
        Entity cam = mc.getRenderViewEntity();
        if(cam == null) return;
        ReplayHandler.addKeyframe(new PositionKeyframe(ReplayHandler.getRealTimelineCursor(), new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw % 360, ReplayHandler.getCameraTilt())));
    }

    private void addTimeKeyframe() {
        ReplayHandler.addKeyframe(new TimeKeyframe(ReplayHandler.getRealTimelineCursor(), ReplayMod.replaySender.currentTimeStamp()));
    }

    private void zoomIn() {
        if(!isClick()) return;
        this.zoom_scale = Math.max(0.025f, zoom_scale - zoom_steps);
    }

    private void zoomOut() {
        if(!isClick()) return;
        this.zoom_scale = Math.min(1f, zoom_scale + zoom_steps);
        this.pos_left = Math.min(pos_left, 1f - zoom_scale);
    }

    private boolean isClick() {
        if(Mouse.isButtonDown(0)) {
            boolean bef = mouseDwn;
            mouseDwn = true;
            return !bef;
        } else {
            mouseDwn = false;
            return false;
        }
    }

    private enum MarkerType {

        ONE_S(1 * 1000, 100),
        FIVE_S(5 * 1000, 1 * 1000),
        QUARTER_M(15 * 1000, 3 * 1000),
        HALF_M(30 * 1000, 5 * 1000),
        ONE_M(60 * 1000, 10 * 1000),
        FIVE_M(5 * 60 * 1000, 50 * 1000);

        int minimum;
        int small_min;
        int maximum = 10;

        MarkerType(int minimum, int small_min) {
            this.minimum = minimum;
            this.small_min = small_min;
        }

        public static MarkerType getMarkerType(float scale, long totalLength) {
            long visible = Math.round((double) totalLength * scale);
            long seconds = visible;

            for(MarkerType mt : values()) {
                if(seconds / mt.getDistance() <= 10) {
                    return mt;
                }
            }

            return FIVE_M;
        }

        int getDistance() {
            return minimum;
        }

        int getSmallDistance() {
            return small_min;
        }
    }

    public void playOrPause() {
        boolean paused = !ReplayMod.replaySender.paused();
        if(paused) {
            ReplayMod.replaySender.setReplaySpeed(0);
        } else {
            ReplayMod.replaySender.setReplaySpeed(speedSlider.getSliderValue());
        }
    }
}
