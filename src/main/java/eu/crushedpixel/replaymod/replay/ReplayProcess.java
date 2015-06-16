package eu.crushedpixel.replaymod.replay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.interpolation.LinearPoint;
import eu.crushedpixel.replaymod.interpolation.LinearTimestamp;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.ReplayTimer;
import eu.crushedpixel.replaymod.video.VideoRenderer;
import net.minecraft.client.Minecraft;

import java.io.IOException;

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
    private static boolean blocked = false;
    private static boolean deepBlock = false;
    private static boolean requestFinish = false;
    private static float lastPartialTicks, lastRenderPartialTicks;
    private static int lastTicks;
    private static boolean resetTimer = false;
    private static boolean firstTime = false;

    public static boolean isVideoRecording() {
        return isVideoRecording;
    }

    private static void resetProcess() {
        firstTime = true;

        lastPosition = null;
        motionSpline = null;
        motionLinear = null;
        timeLinear = null;
        calculated = false;
        requestFinish = false;

        blocked = deepBlock = false;

        startRealTime = System.currentTimeMillis();
        lastRealTime = startRealTime;
        lastRealReplayTime = 0;
        lastTimestamp = -1;
        lastSpeed = 1f;
        linear = ReplayMod.replaySettings.isLinearMovement();

        previousReplaySpeed = ReplayMod.replaySender.getReplaySpeed();

        EnchantmentTimer.resetRecordingTime();
    }

    public static void startReplayProcess(RenderOptions renderOptions) {
        mc.displayGuiScreen(null);

        ReplayHandler.selectKeyframe(null);
        resetProcess();

        isVideoRecording = renderOptions != null;

        ReplayMod.chatMessageHandler.initialize();

        //if not enough keyframes, abort and leave chat message
        if(ReplayHandler.getPosKeyframeCount() < 2 && ReplayHandler.getTimeKeyframeCount() < 2) {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.notenoughkeyframes", ChatMessageType.WARNING);
            return;
        }

        ReplayHandler.sortKeyframes();
        ReplayHandler.setInPath(true);
        ReplayMod.replaySender.setAsyncMode(false);

        if (renderOptions == null) {
            //gets first Timestamp and sets Replay Time to it
            TimeKeyframe tf = ReplayHandler.getFirstTimeKeyframe();
            if (tf != null) {
                int ts = tf.getTimestamp();
                if (ts < ReplayMod.replaySender.currentTimeStamp()) {
                    mc.displayGuiScreen(null);
                }
                ReplayMod.replaySender.sendPacketsTill(ts);
            }

            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathstarted", ChatMessageType.INFORMATION);
            mc.timer.timerSpeed = 1;
        } else {
            try {
                isVideoRecording = true;
                boolean success = new VideoRenderer(renderOptions).renderVideo();
                isVideoRecording = false;
                stopReplayProcess(success);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void stopReplayProcess(boolean finished) {
        if(!ReplayHandler.isInPath()) return;

        //if canceled, display a different chat message
        if(finished) ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathfinished", ChatMessageType.INFORMATION);
        else {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathinterrupted", ChatMessageType.INFORMATION);
        }

        ReplayHandler.setInPath(false);
        ReplayMod.replaySender.setAsyncMode(true);

        ReplayMod.replaySender.stopHurrying();

        ReplayTimer.get(mc).passive = false;
        ReplayMod.replaySender.setReplaySpeed(previousReplaySpeed);
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    public static void unblockAndTick(boolean justCheck) {
        if(!deepBlock) blocked = false;
        if(!blocked || !isVideoRecording())
            ReplayProcess.tickReplay(justCheck);
    }

    //if justCheck is true, no Screenshot will be taken, it will only be checked
    //whether all chunks have been rendered. This is necessary because no Render ticks
    //are called if the Timer speed is set to 0, leading to this method never being
    //called from the RenderWorldLastEvent handlers.
    public static void tickReplay(boolean justCheck) {
        if (isVideoRecording) {
            return;
        }
        if(ReplayMod.replaySender.isHurrying()) {
            lastRealTime = System.currentTimeMillis();
            return;
        }

        if(firstTime) {
            firstTime = false;
            lastPartialTicks = 100;
            lastRenderPartialTicks = 100;
            lastTicks = 100;
            mc.timer.renderPartialTicks = 100;
            mc.timer.elapsedPartialTicks = 100;
            mc.timer.elapsedTicks = 100;
        }

        if(justCheck) return;

        int posCount = ReplayHandler.getPosKeyframeCount();
        int timeCount = ReplayHandler.getTimeKeyframeCount();

        if(!linear && motionSpline == null) {
            //set up spline path
            motionSpline = new SplinePoint();
            for(Keyframe kf : ReplayHandler.getKeyframes()) {
                if(kf instanceof PositionKeyframe) {
                    PositionKeyframe pkf = (PositionKeyframe) kf;
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
                    PositionKeyframe pkf = (PositionKeyframe) kf;
                    Position pos = pkf.getPosition();
                    motionLinear.addPoint(pos);
                }
            }
        }
        if(timeLinear == null) {
            timeLinear = new LinearTimestamp();
            for(Keyframe kf : ReplayHandler.getKeyframes()) {
                if(kf instanceof TimeKeyframe) {
                    timeLinear.addPoint(((TimeKeyframe) kf).getTimestamp());
                }
            }
        }

        if(!calculated) {
            calculated = true;
            if(posCount > 1 && motionSpline != null)
                motionSpline.prepare();
        }

        long curTime = System.currentTimeMillis();
        long timeStep = curTime - lastRealTime;

        int curRealReplayTime = (int) (lastRealReplayTime + timeStep);

        PositionKeyframe lastPos = ReplayHandler.getPreviousPositionKeyframe(curRealReplayTime);
        PositionKeyframe nextPos = ReplayHandler.getNextPositionKeyframe(curRealReplayTime);


        boolean spectating = false;

        //if it's between two spectator keyframes sharing the same entity, spectate this entity
        if(lastPos != null && nextPos != null) {
            if(lastPos.getSpectatedEntityID() != null && nextPos.getSpectatedEntityID() != null) {
                if(lastPos.getSpectatedEntityID().equals(nextPos.getSpectatedEntityID())) {
                    spectating = true;
                }
            }
        }

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
                    else
                        curSpeed = ((double) ((nextTime.getTimestamp() - lastTime.getTimestamp()))) / ((double) ((nextTimeStamp - lastTimeStamp)));
                }

                if(lastTimeStamp == nextTimeStamp) {
                    curSpeed = 0f;
                }
            }
        }

        int currentPosDiff = nextPosStamp - lastPosStamp;
        int currentPos = curRealReplayTime - lastPosStamp;

        float currentPosStepPerc = (float) currentPos / (float) currentPosDiff; //The percentage of the travelled path between the current positions
        if(Float.isInfinite(currentPosStepPerc)) currentPosStepPerc = 0;

        int currentTimeDiff = nextTimeStamp - lastTimeStamp;
        int currentTime = curRealReplayTime - lastTimeStamp;

        float currentTimeStepPerc = (float) currentTime / (float) currentTimeDiff; //The percentage of the travelled path between the current timestamps
        if(Float.isInfinite(currentTimeStepPerc)) currentTimeStepPerc = 0;

        float splinePos = ((float) ReplayHandler.getKeyframeIndex(lastPos) + currentPosStepPerc) / (float) (posCount - 1);
        float timePos = ((float) ReplayHandler.getKeyframeIndex(lastTime) + currentTimeStepPerc) / (float) (timeCount - 1);

        if(!spectating) {
            ReplayHandler.spectateCamera();
            Position pos = null;
            if(posCount > 1) {
                if(!linear) {
                    pos = motionSpline.getPoint(Math.max(0, Math.min(1, splinePos)));
                } else {
                    pos = motionLinear.getPoint(Math.max(0, Math.min(1, splinePos)));
                }
            } else {
                if(posCount == 1) {
                    pos = ReplayHandler.getFirstPositionKeyframe().getPosition();
                }
            }

            if(pos != null) {
                ReplayHandler.setCameraTilt(pos.getRoll());
                ReplayHandler.getCameraEntity().movePath(pos);
            }
        } else {
            ReplayHandler.spectateEntity(mc.theWorld.getEntityByID(lastPos.getSpectatedEntityID()));
        }

        Integer curTimestamp = null;
        if(timeLinear != null && timeCount > 1) {
            curTimestamp = timeLinear.getPoint(Math.max(0, Math.min(1, timePos)));
        }

        if(!isVideoRecording()) ReplayMod.replaySender.setReplaySpeed(curSpeed);
        //if(curSpeed > 0)
        lastSpeed = curSpeed;

        lastPartialTicks = mc.timer.elapsedPartialTicks;
        lastRenderPartialTicks = mc.timer.renderPartialTicks;
        lastTicks = mc.timer.elapsedTicks;

        if(curTimestamp != null)
            ReplayMod.replaySender.sendPacketsTill(curTimestamp);

        //splinePos = (index of last entry + add) / total entries

        lastRealReplayTime = curRealReplayTime;
        lastRealTime = curTime;

        if(requestFinish) {
            stopReplayProcess(true);
            requestFinish = false;
        }

        if((splinePos >= 1 || posCount <= 1) && (timePos >= 1 || timeCount <= 1)) {
            requestFinish = true;
        }
    }
}
