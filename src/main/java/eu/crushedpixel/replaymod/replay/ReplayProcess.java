package eu.crushedpixel.replaymod.replay;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.interpolation.LinearPoint;
import eu.crushedpixel.replaymod.interpolation.LinearTimestamp;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
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

	private static double previousReplaySpeed = 0;

	private static boolean calculated = false;

	private static boolean isVideoRecording = false;

	public static boolean isVideoRecording() {
		return isVideoRecording;
	}

	public static void startReplayProcess(boolean record) {
		isVideoRecording = record;
		lastPosition = null;
		motionSpline = null;
		motionLinear = null;
		timeLinear = null;
		calculated = false;
		requestFinish = false;

		ChatMessageRequests.initialize();
		if(ReplayHandler.getPosKeyframeCount() < 2) {
			ChatMessageRequests.addChatMessage("At least 2 position keyframes required!", ChatMessageType.WARNING);
			return;
		}
		startRealTime = System.currentTimeMillis();
		lastRealTime = startRealTime;
		lastRealReplayTime = 0;
		lastTimestamp = -1;
		linear = ReplayMod.replaySettings.isLinearMovement();
		ReplayHandler.sortKeyframes();
		ReplayHandler.setInPath(true);
		previousReplaySpeed = ReplayHandler.getSpeed();

		TimeKeyframe tf = ReplayHandler.getNextTimeKeyframe(-1);
		if(tf != null) {
			int ts = tf.getTimestamp();
			if(ts < ReplayHandler.getReplayTime()) {
				mc.displayGuiScreen((GuiScreen)null);
			}
			ReplayHandler.setReplayPos(ts);
		}

		ChatMessageRequests.addChatMessage("Replay started!", ChatMessageType.INFORMATION);

		if(isVideoRecording()) {
			ticks = 0;
			MCTimerHandler.setTimerSpeed(1);
			MCTimerHandler.setPassiveTimer();
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					while(ReplayHandler.isInPath()) {
						if(!blocked) {
							mc.addScheduledTask(new Runnable() {
								@Override
								public void run() {
									ReplayProcess.tickReplay();
								}
							});
						} else {
							try {
								Thread.sleep(10);
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			});

			t.start();
		}
	}

	public static void stopReplayProcess(boolean finished) {
		if(!ReplayHandler.isInPath()) return;
		if(finished) ChatMessageRequests.addChatMessage("Replay finished!", ChatMessageType.INFORMATION);
		else ChatMessageRequests.addChatMessage("Replay stopped!", ChatMessageType.INFORMATION);
		ReplayHandler.setInPath(false);
		MCTimerHandler.setActiveTimer();
		ReplayHandler.setSpeed(previousReplaySpeed);
		ReplayHandler.setSpeed(0);
	}

	private static boolean blocked = false;
	private static boolean deepBlock = false;

	private static float ticks = 0;

	private static boolean requestFinish = false;

	public static void unblock() {
		if(!deepBlock) blocked = false;
	}

	public static void tickReplay() {
		if(isVideoRecording()) {
			recordingTick();
		} else {
			normalTick();
		}
	}

	private static void normalTick() {

		if(ReplayHandler.isHurrying()) {
			lastRealTime = System.currentTimeMillis();
			return;
		}

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
			if(motionSpline != null) motionSpline.calcSpline();
		}

		long curTime = System.currentTimeMillis();
		long timeStep = curTime - lastRealTime;

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

		double curSpeed = 0f;

		if(nextTime != null || lastTime != null) {
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
				curSpeed = ((double)((nextTime.getTimestamp()-lastTime.getTimestamp())))/((double)((nextTimeStamp-lastTimeStamp)));
			}

			if(lastTimeStamp == nextTimeStamp) {
				curSpeed = 0f;
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

		float splinePos = ((float)ReplayHandler.getKeyframeIndex(lastPos) + currentPosStepPerc)/(float)(ReplayHandler.getPosKeyframeCount()-1);
		
		float timePos = ((float)ReplayHandler.getKeyframeIndex(lastTime) + currentTimeStepPerc)/(float)(ReplayHandler.getTimeKeyframeCount()-1);
		
		Position pos = null;
		if(!linear) {
			pos = motionSpline.getPoint(Math.max(0, Math.min(1, splinePos)));
		} else {
			pos = motionLinear.getPoint(Math.max(0, Math.min(1, splinePos)));
		}

		Integer curPos = null;
		if(timeLinear != null) {
			curPos = timeLinear.getPoint(Math.max(0, Math.min(1, timePos)));
		}

		if(pos != null) ReplayHandler.getCameraEntity().movePath(pos);

		ReplayHandler.setSpeed(curSpeed);

		if(curPos != null) ReplayHandler.setReplayPos(curPos);

		//splinePos = (index of last entry + add) / total entries

		lastRealReplayTime = curRealReplayTime;
		lastRealTime = curTime;

		if(requestFinish) {
			stopReplayProcess(true);
			requestFinish = false;
		}

		if(splinePos >= 1) {
			requestFinish = true;
		}
	}

	private static void recordingTick() {
		if(ReplayHandler.isHurrying()) {
			if(!isVideoRecording()) {
				lastRealTime = System.currentTimeMillis();
			}
			return;
		}

		if(blocked && isVideoRecording()) {
			return;
		}
		deepBlock = true;
		blocked = true;

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
			motionSpline.calcSpline();
		}

		long timeStep;

		long curTime = System.currentTimeMillis();

		if(isVideoRecording()) {
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

		double curSpeed = 0f;

		if(nextTime != null || lastTime != null) {
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
				curSpeed = ((double)((nextTime.getTimestamp()-lastTime.getTimestamp())))/((double)((nextTimeStamp-lastTimeStamp)));
			}

			if(lastTimeStamp == nextTimeStamp) {
				curSpeed = 0f;
			}
		}

		int currentDiff = nextPosStamp - lastPosStamp;
		int current = curRealReplayTime - lastPosStamp;

		float currentStepPerc = (float)current/(float)currentDiff; //The percentage of the travelled path between the current positions
		if(Float.isInfinite(currentStepPerc)) currentStepPerc = 0;

		float splinePos = ((float)ReplayHandler.getKeyframeIndex(lastPos) + currentStepPerc)/(float)(ReplayHandler.getPosKeyframeCount()-1);
		float timePos = ((float)ReplayHandler.getKeyframeIndex(lastTime) + currentStepPerc)/(float)(ReplayHandler.getTimeKeyframeCount()-1);

		Position pos = null;
		if(!linear) {
			pos = motionSpline.getPoint(Math.max(0, Math.min(1, splinePos)));
		} else {
			pos = motionLinear.getPoint(Math.max(0, Math.min(1, splinePos)));
		}

		Integer curPos = null;
		if(timeLinear != null) {
			curPos = timeLinear.getPoint(Math.max(0, Math.min(1, timePos)));
		}

		if(pos != null) ReplayHandler.getCameraEntity().movePath(pos);

		ReplayHandler.setSpeed((float)curSpeed);
		if(isVideoRecording()) {
			MCTimerHandler.updateTimer((1f/ReplayMod.replaySettings.getVideoFramerate()));
		}
		if(curPos != null) ReplayHandler.setReplayPos(curPos);

		//splinePos = (index of last entry + add) / total entries

		lastRealReplayTime = curRealReplayTime;
		lastRealTime = curTime;

		//Video capturing, for testing purposes
		if(isVideoRecording()) {
			try {
				if(!VideoWriter.isRecording() && ReplayHandler.isInPath()) {
					VideoWriter.startRecording(mc.displayWidth, mc.displayHeight);
				} else {
					VideoWriter.writeImage(ScreenCapture.captureScreen());
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(requestFinish) {
			stopReplayProcess(true);
			VideoWriter.endRecording();
		}

		if(splinePos >= 1) {
			requestFinish = true;
		}

		deepBlock = false;
	}
}
