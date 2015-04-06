package eu.crushedpixel.replaymod.replay;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;
import eu.crushedpixel.replaymod.gui.GuiCancelRender;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.interpolation.LinearPoint;
import eu.crushedpixel.replaymod.interpolation.LinearTimestamp;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.video.ScreenCapture;
import eu.crushedpixel.replaymod.video.VideoWriter;

public class ReplayProcess {

	private static Minecraft mc = Minecraft.getMinecraft();

	private static long startRealTime;
	private static int lastRealReplayTime;
	private static long lastRealTime = 0;

	private static boolean linear = false;

	private static Position lastPosition = null;
	private static int lastTimestamp = -1;

	private static SplinePoint motionSpline = null;
	private static LinearPoint motionLinear = null;
	private static LinearTimestamp timeLinear = null;

	private static double lastSpeed = 1f;

	private static double previousReplaySpeed = 0;

	private static boolean calculated = false;

	private static boolean isVideoRecording = false;

	public static boolean isVideoRecording() {
		return isVideoRecording;
	}

	public static void startReplayProcess(boolean record) {
		ReplayHandler.selectKeyframe(null);

		isVideoRecording = record;
		lastPosition = null;
		motionSpline = null;
		motionLinear = null;
		timeLinear = null;
		calculated = false;
		requestFinish = false;

		ChatMessageRequests.initialize();
		if(ReplayHandler.getPosKeyframeCount() < 2 && ReplayHandler.getTimeKeyframeCount() < 2) {
			ChatMessageRequests.addChatMessage("At least 2 position or time keyframes required!", ChatMessageType.WARNING);
			return;
		}

		blocked = deepBlock = false;

		startRealTime = System.currentTimeMillis();
		lastRealTime = startRealTime;
		lastRealReplayTime = 0;
		lastTimestamp = -1;
		lastSpeed = 1f;
		linear = ReplayMod.replaySettings.isLinearMovement();
		ReplayHandler.sortKeyframes();
		ReplayHandler.setInPath(true);
		previousReplaySpeed = ReplayHandler.getSpeed();

		EnchantmentTimer.resetRecordingTime();

		TimeKeyframe tf = ReplayHandler.getNextTimeKeyframe(-1);
		if(tf != null) {
			int ts = tf.getTimestamp();
			if(ts < ReplayHandler.getReplayTime()) {
				mc.displayGuiScreen(null);
			}
			ReplayHandler.setReplayTime(ts);
		}

		ChatMessageRequests.addChatMessage("Replay started!", ChatMessageType.INFORMATION);

		mc.renderGlobal.loadRenderers();

		if(isVideoRecording()) {
			MCTimerHandler.setTimerSpeed(1f);
			MCTimerHandler.setPassiveTimer();
		}
	}

	public static void stopReplayProcess(boolean finished) {
		if(!ReplayHandler.isInPath()) return;
		if(finished) ChatMessageRequests.addChatMessage("Replay finished!", ChatMessageType.INFORMATION);
		else {
			ChatMessageRequests.addChatMessage("Replay stopped!", ChatMessageType.INFORMATION);
			if(isVideoRecording()) {
				VideoWriter.abortRecording();
			}
		}
		ReplayHandler.setInPath(false);
		MCTimerHandler.setActiveTimer();
		ReplayHandler.setSpeed(previousReplaySpeed);
		//ReplayHandler.setSpeed(0);
	}

	private static boolean blocked = false;
	private static boolean deepBlock = false;

	private static boolean requestFinish = false;

	public static void unblockAndTick(boolean justCheck) {
		if(!deepBlock) blocked = false;
		if(!blocked || !isVideoRecording()) 
			ReplayProcess.tickReplay(justCheck);
	}

	public static void tickReplay(boolean justCheck) {
		pathTick(isVideoRecording(), justCheck);
	}

	private static void pathTick(boolean recording, boolean justCheck) {
		if(ReplayHandler.isHurrying()) {
			lastRealTime = System.currentTimeMillis();
			return;
		}

		if(recording && ((ReplayMod.replaySettings.getWaitForChunks() && RenderChunk.renderChunksUpdated != 0) || mc.currentScreen instanceof GuiCancelRender)) {
			MCTimerHandler.setTimerSpeed(0f);
			MCTimerHandler.setPartialTicks(0f);
			MCTimerHandler.setRenderPartialTicks(0f);
			MCTimerHandler.setTicks(0);
			return;
		} else if (recording && ReplayMod.replaySettings.getWaitForChunks()) {
			MCTimerHandler.setTimerSpeed((float)lastSpeed);
		}


		if(justCheck) return;

		if(recording) {
			if(blocked) return;

			deepBlock = true;
			blocked = true;
		}

		int posCount = ReplayHandler.getPosKeyframeCount();
		int timeCount = ReplayHandler.getTimeKeyframeCount();

		if(!linear && motionSpline == null) {
			//set up spline path
			motionSpline = new SplinePoint();
			for(Keyframe kf : ReplayHandler.getKeyframes()) {
				if(kf instanceof PositionKeyframe) {
					PositionKeyframe pkf = (PositionKeyframe)kf;
					Position pos = pkf.getPosition();
					motionSpline.addPoint(pos);
				}
			}
		}

		if(linear && motionLinear == null) {
			//set up linear path
			motionLinear = new LinearPoint();
			for(Keyframe kf : ReplayHandler.getKeyframes()) {
				if(kf instanceof PositionKeyframe) {
					PositionKeyframe pkf = (PositionKeyframe)kf;
					Position pos = pkf.getPosition();
					motionLinear.addPoint(pos);
				}
			}
		}
		if(timeLinear == null) {
			timeLinear = new LinearTimestamp();
			for(Keyframe kf : ReplayHandler.getKeyframes()) {
				if(kf instanceof TimeKeyframe) {
					timeLinear.addPoint(((TimeKeyframe)kf).getTimestamp());
				}
			}
		}

		if(!calculated) {
			calculated = true;
			if(posCount > 1 && motionSpline != null)
				motionSpline.calcSpline();
		}

		long curTime = System.currentTimeMillis();
		long timeStep;
		if(recording) {
			timeStep = 1000/ReplayMod.replaySettings.getVideoFramerate();
		} else {
			timeStep = curTime - lastRealTime;
		}

		int curRealReplayTime = (int)(lastRealReplayTime + timeStep);

		PositionKeyframe lastPos = ReplayHandler.getPreviousPositionKeyframe(curRealReplayTime);
		PositionKeyframe nextPos = ReplayHandler.getNextPositionKeyframe(curRealReplayTime);

		ReplayHandler.setRealTimelineCursor(curRealReplayTime);

		int lastPosStamp = 0;
		int nextPosStamp = 0;

		if(nextPos != null || lastPos != null) {
			if(nextPos != null) {
				nextPosStamp = nextPos.getRealTimestamp();
			} else {
				nextPosStamp = lastPos.getRealTimestamp();
			}

			if(lastPos != null) {
				lastPosStamp = lastPos.getRealTimestamp();
			} else {
				lastPosStamp = nextPos.getRealTimestamp();
			}
		}

		TimeKeyframe lastTime = ReplayHandler.getPreviousTimeKeyframe(curRealReplayTime);
		TimeKeyframe nextTime = ReplayHandler.getNextTimeKeyframe(curRealReplayTime);

		int lastTimeStamp = 0;
		int nextTimeStamp = 0;

		double curSpeed = 0;

		if(timeCount > 1 && (nextTime != null || lastTime != null)) {

			if(nextTime != null && lastTime != null && nextTime.getRealTimestamp() == lastTime.getRealTimestamp()) {
				curSpeed = 0;
			} else {
				if(nextTime != null) {
					nextTimeStamp = nextTime.getRealTimestamp();
				} else {
					nextTimeStamp = lastTime.getRealTimestamp();
				}

				if(lastTime != null) {
					lastTimeStamp = lastTime.getRealTimestamp();
				} else {
					lastTimeStamp = nextTime.getRealTimestamp();
				}

				if(!(nextTime == null || lastTime == null)) {
					if(lastTimeStamp == nextTimeStamp) curSpeed = 0f;
					else curSpeed = ((double)((nextTime.getTimestamp()-lastTime.getTimestamp())))/((double)((nextTimeStamp-lastTimeStamp)));
				}

				if(lastTimeStamp == nextTimeStamp) {
					curSpeed = 0f;
				}
			}
		}

		int currentPosDiff = nextPosStamp - lastPosStamp;
		int currentPos = curRealReplayTime - lastPosStamp;

		float currentPosStepPerc = (float)currentPos/(float)currentPosDiff; //The percentage of the travelled path between the current positions
		if(Float.isInfinite(currentPosStepPerc)) currentPosStepPerc = 0;

		int currentTimeDiff = nextTimeStamp - lastTimeStamp;
		int currentTime = curRealReplayTime - lastTimeStamp;

		float currentTimeStepPerc = (float)currentTime/(float)currentTimeDiff; //The percentage of the travelled path between the current timestamps
		if(Float.isInfinite(currentTimeStepPerc)) currentTimeStepPerc = 0;

		float splinePos = ((float)ReplayHandler.getKeyframeIndex(lastPos) + currentPosStepPerc)/(float)(posCount-1);
		float timePos = ((float)ReplayHandler.getKeyframeIndex(lastTime) + currentTimeStepPerc)/(float)(timeCount-1);

		Position pos = null;
		if(posCount > 1) {
			if(!linear) {
				pos = motionSpline.getPoint(Math.max(0, Math.min(1, splinePos)));
			} else {
				pos = motionLinear.getPoint(Math.max(0, Math.min(1, splinePos)));
			}
		} else {
			if(posCount == 1) {
				pos = ReplayHandler.getNextPositionKeyframe(-1).getPosition();
			}
		}

		Integer curPos = null;
		if(timeLinear != null && timeCount > 1) {
			curPos = timeLinear.getPoint(Math.max(0, Math.min(1, timePos)));
		}

		if(pos != null) {
			ReplayHandler.getCameraEntity().movePath(pos);
		}

		if(curSpeed > 0) {
			ReplayHandler.setSpeed(curSpeed);
			lastSpeed = curSpeed;
		}

		if(recording) {
			MCTimerHandler.updateTimer((1f/ReplayMod.replaySettings.getVideoFramerate()));
			EnchantmentTimer.increaseRecordingTime((1000/ReplayMod.replaySettings.getVideoFramerate()));
		}

		if(curPos != null && curPos != ReplayHandler.getDesiredTimestamp()) ReplayHandler.setReplayTime(curPos);

		//splinePos = (index of last entry + add) / total entries

		lastRealReplayTime = curRealReplayTime;
		lastRealTime = curTime;

		if(isVideoRecording()) {
			try {
				if(!VideoWriter.isRecording() && ReplayHandler.isInPath()) {
					VideoWriter.startRecording(mc.displayWidth, mc.displayHeight);
				} else {
					final BufferedImage screen = ScreenCapture.captureScreen();
					VideoWriter.writeImage(screen);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(requestFinish) {
			stopReplayProcess(true);
			requestFinish = false;
			if(recording) {
				VideoWriter.endRecording();
			} 
		}

		if((splinePos >= 1 || posCount <= 1) && (timePos >= 1 || timeCount <= 1)) {
			requestFinish = true;
		}

		if(recording) {
			deepBlock = false;
		}
	}
}
