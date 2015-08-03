package eu.crushedpixel.replaymod.replay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.ReplayTimer;
import eu.crushedpixel.replaymod.utils.CameraPathValidator;
import eu.crushedpixel.replaymod.video.VideoRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReplayProcess {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static int lastRealReplayTime;
    private static long lastRealTime = 0;

    private static boolean linear = false;

    private static int initialTimestamp = 0;

    private static double previousReplaySpeed = 0;

    @Getter
    private static VideoRenderer videoRenderer = null;

    private static boolean isVideoRecording = false;
    private static boolean requestFinish = false;
    private static boolean firstTime = false;

    public static boolean isVideoRecording() {
        return isVideoRecording;
    }

    //a copy of the initial sound settings,
    //which we will modify to prevent game sounds from annoying us while rendering

    @SuppressWarnings("unchecked") //I, too, blame Forge for not re-adding generics to that Map.
    private static Map<SoundCategory, Float> mapSoundLevelsBefore = null;

    private static void resetProcess() {
        firstTime = true;

        requestFinish = false;

        lastRealTime = System.currentTimeMillis();
        lastRealReplayTime = 0;
        linear = ReplayMod.replaySettings.isLinearMovement();

        previousReplaySpeed = ReplayMod.replaySender.getReplaySpeed();

        EnchantmentTimer.resetRecordingTime();
    }

    public static void startReplayProcess(RenderOptions renderOptions, boolean fromStart) {
        mc.displayGuiScreen(null);

        ReplayHandler.selectKeyframe(null);
        resetProcess();

        isVideoRecording = renderOptions != null;

        ReplayMod.chatMessageHandler.initialize();

        try {
            CameraPathValidator.validateCameraPath(ReplayHandler.getPositionKeyframes(), ReplayHandler.getTimeKeyframes());
        } catch(CameraPathValidator.InvalidCameraPathException e) {
            e.printToChat();
            return;
        }

        ReplayHandler.setInPath(true);
        ReplayMod.replaySender.setAsyncMode(false);

        //default camera path, no rendering
        if (renderOptions == null) {
            initialTimestamp = fromStart ? 0 : ReplayHandler.getRealTimelineCursor();

            //if the cursor is at (or very near) the end, play from the beginning as well
            if(initialTimestamp + 50 >= Math.max(ReplayHandler.getTimeKeyframes().last().getRealTimestamp(),
                    ReplayHandler.getPositionKeyframes().last().getRealTimestamp())) {
                initialTimestamp = 0;
            }

            lastRealReplayTime = initialTimestamp;

            int ts = ReplayHandler.getTimeKeyframes().getInterpolatedValueForTimestamp(initialTimestamp, true).asInt();
            if (ts < ReplayMod.replaySender.currentTimeStamp()) {
                mc.displayGuiScreen(null);
            }
            ReplayMod.replaySender.sendPacketsTill(ts);

            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathstarted", ChatMessageType.INFORMATION);
            mc.timer.timerSpeed = 1;

            //set the sound level map to null
            //so a previous map doesn't override the current game settings after a camera path
            mapSoundLevelsBefore = null;
        } else {
            //if FBOs are not enabled/supported, prevent the user from resizing the MC window
            if(!OpenGlHelper.isFramebufferEnabled()) {
                Display.setResizable(false);
            }

            //if rendering, disable all game sounds except for gui sounds
            @SuppressWarnings("unchecked") //I, too, blame Forge for not re-adding generics to that Map.
            Map<SoundCategory, Float> orgMap = (Map<SoundCategory, Float>)mc.gameSettings.mapSoundLevels;
            mapSoundLevelsBefore = new HashMap<SoundCategory, Float>(orgMap);

            //turn down for what? to mute all sound of course!
            //the GUI sounds (button clicks etc) are not muted this way.
            for(SoundCategory category : SoundCategory.values()) {
                if(category == SoundCategory.MASTER) continue;
                orgMap.put(category, 0f);
            }

            mc.gameSettings.mapSoundLevels = orgMap;

            initialTimestamp = 0;
            boolean success = false;
            try {
                isVideoRecording = true;
                videoRenderer = new VideoRenderer(renderOptions);
                success = videoRenderer.renderVideo();
            } catch (IOException e) {
                e.printStackTrace();

                GuiErrorScreen errorScreen = new GuiErrorScreen(I18n.format("replaymod.gui.rendering.error.title"),
                        I18n.format("replaymod.gui.rendering.error.message"));
                mc.displayGuiScreen(errorScreen);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Rendering video");
                throw new ReportedException(crashReport);
            } finally {
                isVideoRecording = false;
                stopReplayProcess(success);
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

        //re-enable window resizing after rendering
        Display.setResizable(true);

        //restore the sound settings
        mc.gameSettings.mapSoundLevels = mapSoundLevelsBefore;
    }

    //if justCheck is true, no Screenshot will be taken, it will only be checked
    //whether all chunks have been rendered. This is necessary because no Render ticks
    //are called if the Timer speed is set to 0, leading to this method never being
    //called from the RenderWorldLastEvent handlers.
    public static void tickReplay(boolean justCheck) {
        final long curTime = System.currentTimeMillis();
        KeyframeList<AdvancedPosition> positionKeyframes = ReplayHandler.getPositionKeyframes();
        KeyframeList<TimestampValue> timeKeyframes = ReplayHandler.getTimeKeyframes();

        int curRealReplayTime;

        if (isVideoRecording) {
            return;
        }
        if(ReplayMod.replaySender.isHurrying()) {
            lastRealTime = curTime;
            return;
        }

        if(firstTime) {
            if(RenderChunk.renderChunksUpdated != 0 || mc.currentScreen != null) {
                return;
            }

            lastRealTime = curTime;

            firstTime = false;
            mc.timer.renderPartialTicks = 100;
            mc.timer.elapsedPartialTicks = 100;
            mc.timer.elapsedTicks = 100;

            curRealReplayTime = lastRealReplayTime = initialTimestamp;
        } else {
            long timeStep = curTime - lastRealTime;
            curRealReplayTime = (int) (lastRealReplayTime + timeStep);
        }

        if(justCheck) return;

        Keyframe<AdvancedPosition> lastPos = positionKeyframes.getPreviousKeyframe(curRealReplayTime, true);
        Keyframe<AdvancedPosition> nextPos = positionKeyframes.getNextKeyframe(curRealReplayTime, true);

        boolean spectating = false;

        //if it's between two spectator keyframes sharing the same entity, spectate this entity
        if(lastPos != null && nextPos != null) {
            if(lastPos.getValue().getSpectatedEntityID() != null && nextPos.getValue().getSpectatedEntityID() != null) {
                if(lastPos.getValue().getSpectatedEntityID().equals(nextPos.getValue().getSpectatedEntityID())) {
                    spectating = true;
                }
            }
        }

        ReplayHandler.setRealTimelineCursor(curRealReplayTime);

        Keyframe<TimestampValue> lastTime = timeKeyframes.getPreviousKeyframe(curRealReplayTime, true);
        Keyframe<TimestampValue> nextTime = timeKeyframes.getNextKeyframe(curRealReplayTime, true);

        int lastTimeStamp;
        int nextTimeStamp;

        double curSpeed = 0;

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
                curSpeed = ((double) (((int)nextTime.getValue().value - (int)lastTime.getValue().value))) / ((double) ((nextTimeStamp - lastTimeStamp)));
            }

            if(lastTimeStamp == nextTimeStamp) {
                curSpeed = 0f;
            }
        }

        if(!spectating) {
            ReplayHandler.spectateCamera();
            AdvancedPosition pos = positionKeyframes.getInterpolatedValueForTimestamp(curRealReplayTime, linear);

            if(pos != null) {
                ReplayHandler.setCameraTilt((float)pos.getRoll());
                ReplayHandler.getCameraEntity().movePath(pos);
            }
        } else {
            ReplayHandler.spectateEntity(mc.theWorld.getEntityByID(lastPos.getValue().getSpectatedEntityID()));
        }

        Integer curTimestamp = timeKeyframes.getInterpolatedValueForTimestamp(curRealReplayTime, true).asInt();

        if(!isVideoRecording()) ReplayMod.replaySender.setReplaySpeed(curSpeed);

        ReplayMod.replaySender.sendPacketsTill(curTimestamp);

        lastRealReplayTime = curRealReplayTime;
        lastRealTime = curTime;

        if(requestFinish) {
            stopReplayProcess(true);
            requestFinish = false;
        }

        if(curRealReplayTime > timeKeyframes.last().getRealTimestamp()
                && curRealReplayTime > positionKeyframes.last().getRealTimestamp()) {
            requestFinish = true;
        }
    }
}
