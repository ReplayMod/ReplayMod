
package eu.crushedpixel.replaymod.events;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.entities.CameraEntity.MoveDirection;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.gui.GuiReplaySpeedSlider;
import eu.crushedpixel.replaymod.gui.GuiSpectateSelection;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.registry.ReplayGuiRegistry;
import eu.crushedpixel.replaymod.replay.MCTimerHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.video.ReplayScreenshot;
import eu.crushedpixel.replaymod.video.VideoWriter;

public class GuiReplayOverlay extends Gui {

	private Minecraft mc = Minecraft.getMinecraft();

	private int sliderX = 35;
	private int sliderY = 10;

	private int timelineX = sliderX+100+5;
	private int realTimelineX = 10 + 4*25;
	private int realTimelineY = 33+10;

	private int ppButtonX = 10;
	private int ppButtonY = 10;

	private int r_ppButtonX = 10;
	private int r_ppButtonY = realTimelineY+1;

	private int exportButtonX = 35;
	private int exportButtonY = realTimelineY+1;

	private int place_ButtonX = 60;
	private int place_ButtonY = realTimelineY+1;

	private int time_ButtonX = 85;
	private int time_ButtonY = realTimelineY+1;

	private long lastSystemTime = System.currentTimeMillis();

	private ResourceLocation guiLocation = new ResourceLocation("replaymod", "replay_gui.png");
	private ResourceLocation keyframeLocation = new ResourceLocation("replaymod", "extended_gui.png");
	private ResourceLocation timelineLocation = new ResourceLocation("replaymod", "timeline_icons.png");

	private GuiReplaySpeedSlider speedSlider;

	private boolean mouseDown = false;

	private static boolean requestScreenshot = false;

	//private Field drawBlockOutline;

	public static void requestScreenshot() {
		requestScreenshot = true;
	}

	public GuiReplayOverlay() {
		try {
//			drawBlockOutline = EntityRenderer.class.getDeclaredField(MCPNames.field("field_175073_D"));
	//		drawBlockOutline.setAccessible(true);
		//	drawBlockOutline.set(Minecraft.getMinecraft().entityRenderer, false);
		} catch(Exception e) {}
	}

	//@SubscribeEvent TODO
	public void renderHand(RenderHandEvent event) {
		if(ReplayHandler.replayActive()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void tick(TickEvent event) {
		if(!ReplayHandler.replayActive()) return;
		if(ReplayHandler.isReplaying() && !ReplayProcess.isVideoRecording()) ReplayProcess.tickReplay();
		ReplayProcess.unblock();
		if(ReplayHandler.getCameraEntity() != null)
			ReplayHandler.getCameraEntity().updateMovement();
		if(!ReplayHandler.isReplaying()) onMouseMove(new MouseEvent());
		FMLCommonHandler.instance().bus().post(new InputEvent.KeyInputEvent());
	}

	private double lastX, lastY, lastZ;
	private float lastPitch, lastYaw;

	@SubscribeEvent
	public void onRenderWorld(RenderWorldLastEvent event) 
			throws IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, IOException {
		if(!ReplayHandler.replayActive()) return;
		if(ReplayHandler.isCamera()) ReplayHandler.setCameraEntity(ReplayHandler.getCameraEntity());
		if(ReplayHandler.replayActive() && ReplayHandler.isPaused()) {
			if(mc != null && mc.thePlayer != null)
				MinecraftTicker.runMouseKeyboardTick(mc);
		}
		if(mc.getRenderViewEntity() == mc.thePlayer || !mc.getRenderViewEntity().isEntityAlive() 
				&& ReplayHandler.getCameraEntity() != null) {
			ReplayHandler.spectateCamera();
			ReplayHandler.getCameraEntity().movePath(new Position(lastX, lastY, lastZ, lastPitch, lastYaw));
		} else if(!ReplayHandler.isCamera()) {
			lastX = mc.getRenderViewEntity().posX;
			lastY = mc.getRenderViewEntity().posY;
			lastZ = mc.getRenderViewEntity().posZ;
			lastPitch = mc.getRenderViewEntity().rotationPitch;
			lastYaw = mc.getRenderViewEntity().rotationYaw;
		}
		if(requestScreenshot) {
			requestScreenshot = false;
			ReplayScreenshot.saveScreenshot(mc.getFramebuffer());
		}
	}

	public void resetUI() throws Exception {
		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			mc.displayGuiScreen((GuiScreen)null);
		}
		ReplayHandler.setRealTimelineCursor(0);
		speedSlider = new GuiReplaySpeedSlider(1, sliderX, sliderY, "Speed");
	}

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent event) {
		if(VideoWriter.isRecording()) {
			event.setCanceled(true);
		}
	}


	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Post event) throws IllegalArgumentException, IllegalAccessException {
		
		if(!ReplayHandler.replayActive() || FMLClientHandler.instance().isGUIOpen(GuiSpectateSelection.class) || VideoWriter.isRecording()) {
			return;
		}

		//System.out.println(System.currentTimeMillis()+" | "+MCTimerHandler.getTicks()+" | "+MCTimerHandler.getPartialTicks());

		if(!ReplayGuiRegistry.hidden) ReplayGuiRegistry.hide();

		if(event.type == ElementType.PLAYER_LIST) {
			if(event.isCancelable()) {
				event.setCanceled(true);
			}
			return;
		}

		GL11.glEnable(GL11.GL_BLEND);
	//	drawBlockOutline.set(Minecraft.getMinecraft().entityRenderer, false);

		if(!ReplayHandler.replayActive()) {
			return;
		}

		ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		final int width = sr.getScaledWidth();
		final int height = sr.getScaledHeight();

		final int mouseX = (Mouse.getX() * width / mc.displayWidth);
		final int mouseY = (height - Mouse.getY() * height / mc.displayHeight);

		//Draw Timeline
		drawTimeline(timelineX, width - 14, 9);
		drawRealTimeline(realTimelineX, width - 14 - 11, realTimelineY, mouseX, mouseY);

		//Play/Pause button
		int x = 0;
		int y = 0;

		boolean play = !ReplayHandler.isPaused();
		boolean hover = false;

		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= ppButtonX && mouseX <= ppButtonX+20
					&& mouseY >= ppButtonY && mouseY <= ppButtonY+20) {
				hover = true;
			}
		}

		if(play) {
			y = 20;
		}
		if(hover) {
			x = 20;
		}

		mc.renderEngine.bindTexture(guiLocation);

		GlStateManager.resetColor();
		this.drawModalRectWithCustomSizedTexture(ppButtonX, ppButtonY, x, y, 20, 20, 64, 64);

		//GlStateManager.resetColor();

		//When hurrying, no Timeline jumping etc. is possible
		if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class) && !ReplayHandler.isHurrying()) { //clicking the Button
			speedSlider.mousePressed(mc, mouseX, mouseY);
			if(!mouseDown) {
				mouseDown = true;
				if(hover) {
					boolean paused = !ReplayHandler.isPaused();
					if(paused) {
						ReplayHandler.setSpeed(0);
					} else {
						ReplayHandler.setSpeed(speedSlider.getSliderValue());
					}

				} else if(mouseX >= exportButtonX && mouseX <= exportButtonX+20 && mouseY >= exportButtonY && exportButtonY <= exportButtonY+20) {
					ReplayHandler.startPath(true);
				}

				if(mouseX >= timelineX+4 && mouseX <= width - 18 && mouseY >= 11 && mouseY <= 29) {
					double tot = (width - 18)-(timelineX+4);
					double perc = (mouseX-(timelineX+4))/tot;
					double time = perc*(double)ReplayHandler.getReplayLength();

					if(time < ReplayHandler.getReplayTime()) {
						mc.displayGuiScreen((GuiScreen)null);
					}

					CameraEntity cam = ReplayHandler.getCameraEntity();
					if(cam != null) {
						ReplayHandler.setLastPosition(new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw));
					} else {
						ReplayHandler.setLastPosition(null);
					}

					ReplayHandler.setReplayPos((int)time);
				}
			}

		} else {
			try {
				speedSlider.mouseReleased(mouseX, mouseY);
				mouseDown = false;
			} catch(Exception e) {}
		}

		//TODO: Save Video Button
		hover = false;
		x = 0;
		y = 18;

		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= exportButtonX && mouseX <= exportButtonX+20
					&& mouseY >= exportButtonY && mouseY <= exportButtonY+20) {
				hover = true;
			}
		}

		if(hover) {
			x = 20;
		}

		mc.renderEngine.bindTexture(timelineLocation);

		GlStateManager.resetColor();
		this.drawModalRectWithCustomSizedTexture(exportButtonX, exportButtonY, x, y, 20, 20, 64, 64);

		//GlStateManager.resetColor();

		//Place Keyframe Button
		hover = false;
		x = 0;
		y = 0;

		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= place_ButtonX && mouseX <= place_ButtonX+20
					&& mouseY >= place_ButtonY && mouseY <= place_ButtonY+20) {
				hover = true;
			}
		}

		if(hover) {
			x = 20;
		}

		if(ReplayHandler.getSelected() != null && ReplayHandler.getSelected() instanceof PositionKeyframe) {
			y += 20;
		}

		mc.renderEngine.bindTexture(keyframeLocation);

		if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
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
			if(mouseX >= time_ButtonX && mouseX <= time_ButtonX+20
					&& mouseY >= time_ButtonY && mouseY <= time_ButtonY+20) {
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

		mc.renderEngine.bindTexture(keyframeLocation);

		if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(ReplayHandler.getSelected() == null || !(ReplayHandler.getSelected() instanceof TimeKeyframe)) {
				addTimeKeyframe();
			} else {
				ReplayHandler.removeKeyframe(ReplayHandler.getSelected());
			}
		}

		GlStateManager.resetColor();
		this.drawModalRectWithCustomSizedTexture(time_ButtonX, time_ButtonY, x, y, 20, 20, 64, 64);

		if(mouseX >= (timelineX+4) && mouseX <= width - 18 && mouseY >= 11 && mouseY <= 29) {
			double tot = (width - 18)-(timelineX+4);
			double perc = (mouseX-(timelineX+4))/tot;
			long time = Math.round(perc*(double)ReplayHandler.getReplayLength());

			String timestamp = (String.format("%02d:%02ds",
					TimeUnit.MILLISECONDS.toMinutes(time),
					TimeUnit.MILLISECONDS.toSeconds(time) - 
					TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
					));

			this.drawCenteredString(mc.fontRendererObj, timestamp, mouseX, mouseY+5, Color.WHITE.getRGB());
		}

		if(mc.inGameHasFocus) {
			Mouse.setCursorPosition(width/2, height/2);
		}

		try {
			speedSlider.drawButton(mc, mouseX, mouseY);
		} catch(Exception e) {}

		GlStateManager.resetColor();

		Entity player = ReplayHandler.getCameraEntity();
		if(player != null) {
			player.setVelocity(0, 0, 0);
		}

		if(!Mouse.isButtonDown(0)) isClick();
	}

	private int tl_begin_x=0;
	private int tl_begin_width = 4;

	private int tl_end_x=60;
	private int tl_end_width = 4;

	private int tl_middle_x=4;

	private int tl_y=40;

	private void drawTimeline(int minX, int maxX, int y) {
		int zero = minX+tl_begin_width;
		int full = maxX-tl_end_width;

		GlStateManager.resetColor();
		mc.renderEngine.bindTexture(guiLocation);
		this.drawModalRectWithCustomSizedTexture(minX, y, tl_begin_x, tl_y, tl_begin_width, 22, 64, 64);

		for(int i=minX+tl_begin_width; i<maxX-tl_end_width; i += tl_end_x-tl_begin_width) {
			this.drawModalRectWithCustomSizedTexture(i, y, tl_begin_x+tl_begin_width
					, tl_y, Math.min(tl_end_x-tl_begin_width, maxX-tl_end_width-i)
					, 22, 64, 64);
		}

		this.drawModalRectWithCustomSizedTexture(maxX-tl_end_width, y, tl_end_x, tl_y, tl_end_width, 22, 64, 64);

		//Cursor
		double width = full-zero;
		double perc = (double)ReplayHandler.getReplayTime()/(double)ReplayHandler.getReplayLength();

		int cursorX = (int)Math.round(zero+(perc*width));
		this.drawModalRectWithCustomSizedTexture(cursorX-3, y+3, 44, 0, 8, 16, 64, 64);
	}

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

	private float zoom_scale = 0.1f; //can see 1/10th of the timeline
	private float pos_left = 0f; //left border of timeline is at 0%
	private float cursor_pos = 0f; //cursor is at 0%
	private long timelineLength = 10*60*1000; //10 min of timeline

	private float zoom_steps = 0.05f;

	private boolean wasSliding = false;

	private void drawRealTimeline(int minX, int maxX, int y, int mouseX, int mouseY) {
		int zero = minX+tl_begin_width;
		int full = maxX-tl_end_width;

		//the real timeline
		GlStateManager.resetColor();
		mc.renderEngine.bindTexture(guiLocation);
		this.drawModalRectWithCustomSizedTexture(minX, y, tl_begin_x, tl_y, tl_begin_width, 22, 64, 64);

		for(int i=minX+tl_begin_width; i<maxX-tl_end_width; i += tl_end_x-tl_begin_width) {
			this.drawModalRectWithCustomSizedTexture(i, y, tl_begin_x+tl_begin_width
					, tl_y, Math.min(tl_end_x-tl_begin_width, maxX-tl_end_width-i)
					, 22, 64, 64);
		}

		this.drawModalRectWithCustomSizedTexture(maxX-tl_end_width, y, tl_end_x, tl_y, tl_end_width, 22, 64, 64);

		//Time Slider
		int yo = y+22+1;
		GlStateManager.resetColor();
		mc.renderEngine.bindTexture(timelineLocation);
		this.drawModalRectWithCustomSizedTexture(minX, yo, sl_begin_x, sl_y, 2, 9, 64, 64);

		for(int i=minX+2; i<maxX-1; i+= sl_end_x-2) {
			this.drawModalRectWithCustomSizedTexture(i, yo, 2, sl_y,
					Math.min(sl_end_x-2, maxX-1-i), 9, 64, 64);
		}

		this.drawModalRectWithCustomSizedTexture(maxX-1, yo, sl_end_x, sl_y, 1, 9, 64, 64);

		//Timeline Pos Slider
		int sl_y = yo+1;
		int minPos = minX+1;
		int maxPos = maxX-2;
		int tlWidth = maxPos - minPos;

		int slider_min = minPos+Math.round(pos_left*tlWidth);
		int slider_width = Math.round(zoom_scale*tlWidth);

		int sl_max = slider_min+slider_width;

		this.drawModalRectWithCustomSizedTexture(slider_min, sl_y, slider_begin_x, slider_y, slider_begin_width, slider_height, 64, 64);

		for(int i=slider_min+slider_begin_width; i<sl_max-slider_end_width; i+=slider_end_x-slider_begin_width-slider_begin_x) {
			this.drawModalRectWithCustomSizedTexture(i, sl_y, slider_begin_x+slider_begin_width, slider_y,
					Math.min(slider_end_x-slider_end_width-slider_begin_x, sl_max-slider_end_width-i), slider_height, 64, 64);
		}

		this.drawModalRectWithCustomSizedTexture(sl_max-slider_end_width, sl_y, slider_end_x, slider_y, 
				slider_end_width, slider_height, 64, 64);

		//Slider dragging
		if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class) && (mouseX >= slider_min && mouseX <= sl_max && mouseY >= sl_y && mouseY <= sl_y+slider_height || wasSliding)) {
			wasSliding = true;
			float dx = ((float)Mouse.getDX() * (float)new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth() / mc.displayWidth);
			this.pos_left = Math.min(1f-this.zoom_scale, Math.max(0f, this.pos_left+(dx/(float)tlWidth)));
		}

		if(!Mouse.isButtonDown(0)) {
			wasSliding = false;
		}

		//Timeline Buttons
		//+- Buttons
		boolean hover = false;
		int px = plus_x;
		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= maxX+2 && mouseX <= maxX+2+9
					&& mouseY >= y+1 && mouseY <= y+1+9) {
				hover = true;
			}
		}

		if(hover) {
			px+=9;
		}

		this.drawModalRectWithCustomSizedTexture(maxX+2, y+1, px, plus_y, 9, 9, 64, 64);

		if(hover && Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			zoomIn();
		}

		hover = false;
		int mx = minus_x;

		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= maxX+2 && mouseX <= maxX+2+9
					&& mouseY >= y+9+3 && mouseY <= y+9+3+9) {
				hover = true;
			}
		}

		if(hover) {
			mx+=9;
		}

		this.drawModalRectWithCustomSizedTexture(maxX+2, y+9+3, mx, minus_y, 9, 9, 64, 64);

		if(hover && Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			zoomOut();
		}

		//show Time String
		if(mouseX >= zero && mouseX <= full && mouseY >= y && mouseY <= y+22) {
			long tot = Math.round((double)timelineLength*zoom_scale);
			double perc = (mouseX-(realTimelineX+4))/(double)(full-zero);

			long time = Math.round(this.pos_left*(double)timelineLength)+Math.round(perc*(double)tot);

			String timestamp = (String.format("%02d:%02ds",
					TimeUnit.MILLISECONDS.toMinutes(time),
					TimeUnit.MILLISECONDS.toSeconds(time) - 
					TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
					));
			this.drawCenteredString(mc.fontRendererObj, timestamp, mouseX, mouseY+5, Color.WHITE.getRGB());
		}

		//draw Markers on timeline
		MarkerType mt = MarkerType.getMarkerType(zoom_scale, timelineLength);

		//every x seconds, draw small marker
		long left_real = Math.round(pos_left*(double)timelineLength);
		long right_real = left_real+(Math.round(zoom_scale*timelineLength));
		long tot = Math.round((double)timelineLength*zoom_scale);

		for(int s=0; s<=timelineLength; s+= mt.getSmallDistance()) {
			if(s > right_real) break;
			if(s >= left_real) {
				//calculate absolute position on screen
				long relative = (s) - (left_real);
				double perc = ((double)relative/(double)tot);

				long real_width = full-zero;
				long rel_x = Math.round(perc*(double)real_width);

				long real_x = zero+rel_x;

				this.drawVerticalLine((int)real_x, y+19-3, y+19, Color.WHITE.getRGB());
			}
		}

		//every x seconds, draw big marker
		for(int s=0; s<=timelineLength; s+= mt.getDistance()) {
			if(s > right_real) break;
			if(s >= left_real) {
				//calculate absolute position on screen
				long relative = s - (left_real);
				double perc = ((double)relative/(double)tot);

				long real_width = full-zero;
				long rel_x = Math.round(perc*(double)real_width);

				long real_x = zero+rel_x;

				this.drawVerticalLine((int)real_x, y+19-7, y+19, Color.LIGHT_GRAY.getRGB());

				//write text
				int time = s;
				String timestamp = (String.format("%02d:%02d",
						TimeUnit.MILLISECONDS.toMinutes(time),
						TimeUnit.MILLISECONDS.toSeconds(time) - 
						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
						));

				this.drawCenteredString(mc.fontRendererObj, timestamp, (int)real_x, y-8, Color.WHITE.getRGB());
			}
		}

		//handle Mouse clicks on realTimeLine
		if(Mouse.isButtonDown(0) && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class) && !wasSliding && mouseX >= minX+tl_begin_width && mouseX <= maxX-tl_end_width &&
				mouseY >= y && mouseY <= y+22) {

			//calculate real time and set cursor accordingly
			int width = (maxX-tl_end_width) - (minX+tl_begin_width);
			int rel_x = mouseX-(minX+tl_begin_width);

			float rel_pos = (float)rel_x/(float)width;

			float abs_width = (zoom_scale*(float)timelineLength);
			int real_pos = Math.round(left_real+((rel_pos)*abs_width));

			ReplayHandler.setRealTimelineCursor(real_pos);

			//Keyframe click handling here
			if(isClick()) {
				//tolerance is 2 pixels multiplied with the timespan of one pixel
				int tolerance = 2*Math.round(abs_width/(float)width);

				if(mouseY >= y+9) {
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
			long rel_pos = ReplayHandler.getRealTimelineCursor()-left_real;
			long rel_width = right_real-left_real;
			double perc = (double)rel_pos/(double)rel_width;

			int real_width = (maxX-tl_end_width) - (minX+tl_begin_width);
			double rel_x = (float)real_width*perc;

			int real_x = (int)Math.round((minX+tl_begin_width)+rel_x);
			mc.renderEngine.bindTexture(this.guiLocation);

			GL11.glEnable(GL11.GL_BLEND);
			this.drawModalRectWithCustomSizedTexture(real_x-3, y+3, 44, 0, 8, 16, 64, 64);
			//this.drawModalRectWithCustomSizedTexture(real_x, sl_y, u, v, width, height, textureWidth, textureHeight)
		}


		//Draw Keyframe logos
		mc.renderEngine.bindTexture(timelineLocation);
		for(Keyframe kf : ReplayHandler.getKeyframes()) {
			if(kf.getRealTimestamp() > right_real) break;
			if(kf.getRealTimestamp() >= left_real) {
				int dx = 18;
				int dy = 0;

				int ry = y+3;

				if(kf instanceof TimeKeyframe) {
					dy = 5;
					ry += 5;
				}
				if(ReplayHandler.isSelected(kf)) {
					dx += 5;
				}

				long relative = kf.getRealTimestamp() - (left_real);
				double perc = ((double)relative/(double)tot);

				long real_width = full-zero;
				long rel_x = Math.round(perc*(double)real_width);

				long real_x = zero+rel_x - 2;

				this.drawModalRectWithCustomSizedTexture((int)real_x, ry, dx, dy, 5, 5, 64, 64);
			}
		}

		//Draw Play/Pause Button
		//Play/Pause button

		int dx = 0;
		int dy = 0;

		boolean play = ReplayHandler.isReplaying();
		hover = false;

		if(FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(mouseX >= r_ppButtonX && mouseX <= r_ppButtonX+20
					&& mouseY >= r_ppButtonY && mouseY <= r_ppButtonY+20) {
				hover = true;
			}
		}

		if(play) {
			dy = 20;
		}
		if(hover) {
			dx = 20;
		}

		mc.renderEngine.bindTexture(guiLocation);

		GlStateManager.resetColor();
		this.drawModalRectWithCustomSizedTexture(r_ppButtonX, r_ppButtonY, dx, dy, 20, 20, 64, 64);

		//Handling the click on the Replay starter
		if(hover && Mouse.isButtonDown(0) && isClick() && FMLClientHandler.instance().isGUIOpen(GuiMouseInput.class)) {
			if(ReplayHandler.isReplaying()) {
				ReplayHandler.interruptReplay();
			} else {
				ReplayHandler.startPath(false);
			}
		}

	}

	private void addPlaceKeyframe() {
		Entity cam = mc.getRenderViewEntity();
		if(cam == null) return;
		ReplayHandler.addKeyframe(new PositionKeyframe(ReplayHandler.getRealTimelineCursor(), new Position(cam.posX, cam.posY, cam.posZ, cam.rotationPitch, cam.rotationYaw)));
	}

	private void addTimeKeyframe() {
		ReplayHandler.addKeyframe(new TimeKeyframe(ReplayHandler.getRealTimelineCursor(), ReplayHandler.getReplayTime()));
	}

	private enum MarkerType {

		ONE_S(1*1000, 100),
		FIVE_S(5*1000, 1*1000),
		QUARTER_M(15*1000, 3*1000),
		HALF_M(30*1000, 5*1000),
		ONE_M(60*1000, 10*1000),
		FIVE_M(5*60*1000, 50*1000);

		int minimum;
		int small_min;
		int maximum = 10;

		int getDistance() {
			return minimum;
		}

		int getSmallDistance() {
			return small_min;
		}

		MarkerType(int minimum, int small_min) {
			this.minimum = minimum;
			this.small_min = small_min;
		}

		public static MarkerType getMarkerType(float scale, long totalLength) {
			long visible = Math.round((double)totalLength*scale);
			long seconds = visible;

			for(MarkerType mt : values()) {
				if(seconds/mt.getDistance() <= 10) {
					return mt;
				}
			}

			return FIVE_M;
		}
	}

	private void zoomIn() {
		if(!isClick()) return;
		this.zoom_scale = Math.max(0.025f, zoom_scale-zoom_steps);
	}

	private void zoomOut() {
		if(!isClick()) return;
		this.zoom_scale = Math.min(1f, zoom_scale+zoom_steps);
		this.pos_left = Math.min(pos_left, 1f-zoom_scale);
	}

	private boolean mouseDwn = false;

	private boolean isClick() {
		if(Mouse.isButtonDown(0)) {
			boolean bef = new Boolean(mouseDwn);
			mouseDwn = true;
			return !bef;
		} else {
			mouseDwn = false;
			return false;
		}
	}

	@SubscribeEvent
	public void onMouseMove(MouseEvent event) {
		if(!ReplayHandler.replayActive()) return;
		boolean flag = Display.isActive();
		flag = true;

		mc.mcProfiler.startSection("mouse");

		if (flag && Minecraft.isRunningOnMac && mc.inGameHasFocus && !Mouse.isInsideWindow())
		{
			Mouse.setGrabbed(false);
			Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
			Mouse.setGrabbed(true);
		}

		if (mc.inGameHasFocus && flag)
		{
			mc.mouseHelper.mouseXYChange();
			float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
			float f2 = f1 * f1 * f1 * 8.0F;
			float f3 = (float)mc.mouseHelper.deltaX * f2;
			float f4 = (float)mc.mouseHelper.deltaY * f2;
			byte b0 = 1;

			if (mc.gameSettings.invertMouse)
			{
				b0 = -1;
			}

			ReplayHandler.getCameraEntity().setAngles(f3, f4 * (float)b0);

		}
	}
}
