package eu.crushedpixel.replaymod.replay;

import com.mojang.authlib.GameProfile;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.assets.AssetRepository;
import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.assets.CustomObjectRepository;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.events.KeyframesModifyEvent;
import eu.crushedpixel.replaymod.events.ReplayExitEvent;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.*;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.registry.PlayerHandler;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReplayHandler {

    public static long lastExit = 0;
    private static NetworkManager networkManager;
    private static Minecraft mc = Minecraft.getMinecraft();
    private static EmbeddedChannel channel;
    private static int realTimelinePosition = 0;

    private static Keyframe selectedKeyframe;

    private static boolean inPath = false;

    private static KeyframeList<AdvancedPosition> positionKeyframes = new KeyframeList<AdvancedPosition>();
    private static KeyframeList<TimestampValue> timeKeyframes = new KeyframeList<TimestampValue>();

    private static boolean inReplay = false;
    private static AdvancedPosition lastPosition = null;

    private static KeyframeList<Marker> initialMarkers = new KeyframeList<Marker>();
    private static KeyframeList<Marker> markerKeyframes = new KeyframeList<Marker>();

    private static float cameraTilt = 0;

    private static KeyframeSet[] keyframeRepository = new KeyframeSet[]{};

    @Getter @Setter
    private static AssetRepository assetRepository;

    private static CustomObjectRepository customImageObjects = new CustomObjectRepository();

    /**
     * The file currently being played.
     */
    private static ReplayFile currentReplayFile;

    public static KeyframeSet[] getKeyframeRepository() {
        return keyframeRepository;
    }

    public static void setKeyframeRepository(KeyframeSet[] repo, boolean write) {
        keyframeRepository = repo;
        if(write) {
            try {
                File tempFile = File.createTempFile(ReplayFile.ENTRY_PATHS, "json");

                ReplayFileIO.write(repo, tempFile);

                ReplayMod.replayFileAppender.registerModifiedFile(tempFile, ReplayFile.ENTRY_PATHS, getReplayFile());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static KeyframeList<Marker> getMarkerKeyframes() {
        return markerKeyframes;
    }

    public static void setMarkers(KeyframeList<Marker> m, boolean write) {
        markerKeyframes.clear();
        markerKeyframes.addAll(m);

        if(write) {
            try {
                File tempFile = File.createTempFile(ReplayFile.ENTRY_MARKERS, "json");

                ReplayFileIO.write(m, tempFile);

                ReplayMod.replayFileAppender.registerModifiedFile(tempFile, ReplayFile.ENTRY_MARKERS, getReplayFile());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void useKeyframePresetFromRepository(int index) {
        useKeyframePreset(keyframeRepository[index]);
    }

    public static void useKeyframePreset(KeyframeSet keyframeSet) {
        setCustomImageObjects(Arrays.asList(keyframeSet.getCustomObjects()));

        Keyframe[] kfs = keyframeSet.getKeyframes();

        positionKeyframes.clear();
        timeKeyframes.clear();
        for(Keyframe kf : kfs) {
            addKeyframe(kf);
        }

        selectKeyframe(null);

        fireKeyframesModifyEvent();
    }

    public static void spectateEntity(Entity e) {
        getCameraEntity().spectate(e);
    }

    public static void spectateCamera() {
        spectateEntity(null);
    }

    public static boolean isCamera() {
        return mc.thePlayer instanceof CameraEntity && mc.thePlayer == mc.getRenderViewEntity();
    }

    public static void startPath(RenderOptions renderOptions, boolean fromStart) {
        if(!ReplayHandler.isInPath()) {
            try {
                ReplayProcess.startReplayProcess(renderOptions, fromStart);
            } catch (ReportedException e) {
                // We have to manually unwrap OOM errors as Minecraft doesn't handle them when they're wrapped
                Throwable prevCause = null;
                Throwable cause = e;
                while (cause != null && cause != prevCause) {
                    if (cause instanceof OutOfMemoryError) {
                        // Nevertheless save the crash report in case we actually need it
                        Minecraft minecraft = Minecraft.getMinecraft();
                        CrashReport crashReport = e.getCrashReport();
                        minecraft.addGraphicsAndWorldToCrashReport(crashReport);
                        Bootstrap.printToSYSOUT(crashReport.getCompleteReport());
                        File folder = new File(minecraft.mcDataDir, "crash-reports");
                        File file = new File(folder, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
                        crashReport.saveToFile(file);
                        throw (OutOfMemoryError) cause;
                    }
                    prevCause = cause;
                    cause = e.getCause();
                }
                throw e;
            }
        }
    }

    public static void interruptReplay() {
        ReplayProcess.stopReplayProcess(false);
    }

    public static boolean isInPath() {
        return inPath;
    }

    public static void setInPath(boolean replaying) {
        inPath = replaying;
    }

    public static CameraEntity getCameraEntity() {
        return mc.thePlayer instanceof CameraEntity ? (CameraEntity) mc.thePlayer : null;
    }

    public static float getCameraTilt() {
        return cameraTilt;
    }

    public static void setCameraTilt(float tilt) {
        cameraTilt = tilt;
    }

    public static void addCameraTilt(float tilt) {
        cameraTilt += tilt;
    }

    public static void toggleMarker() {
        if(selectedKeyframe != null && selectedKeyframe.getValue() instanceof Marker) {
            markerKeyframes.remove(selectedKeyframe);
            ReplayHandler.selectKeyframe(null);
        } else {
            AdvancedPosition pos = new AdvancedPosition(mc.getRenderViewEntity(), false);
            int timestamp = ReplayMod.replaySender.currentTimeStamp();
            Keyframe<Marker> markerKeyframe = new Keyframe<Marker>(timestamp, new Marker(null, pos));
            markerKeyframes.add(markerKeyframe);
            ReplayHandler.selectKeyframe(markerKeyframe);
        }
    }

    public static void addTimeKeyframe(Keyframe<TimestampValue> keyframe) {
        timeKeyframes.add(keyframe);
        selectKeyframe(keyframe);

        fireKeyframesModifyEvent();
    }

    public static void addPositionKeyframe(Keyframe<AdvancedPosition> keyframe) {
        positionKeyframes.add(keyframe);
        selectKeyframe(keyframe);

        fireKeyframesModifyEvent();
    }

    @SuppressWarnings("unchecked")
    public static void addKeyframe(Keyframe keyframe) {
        if(keyframe.getValue() instanceof AdvancedPosition) {
            addPositionKeyframe(keyframe);
        } else if(keyframe.getValue() instanceof TimestampValue) {
            addTimeKeyframe(keyframe);
        }
    }

    public static void removeKeyframe(Keyframe keyframe) {
        if(keyframe.getValue() instanceof AdvancedPosition) {
            positionKeyframes.remove(keyframe);
        } else if(keyframe.getValue() instanceof TimestampValue) {
            timeKeyframes.remove(keyframe);
        } else if(keyframe.getValue() instanceof Marker) {
            markerKeyframes.remove(keyframe);
        }

        if(keyframe == selectedKeyframe) {
            selectKeyframe(null);
        }

        fireKeyframesModifyEvent();
    }

    public static KeyframeList<AdvancedPosition> getPositionKeyframes() {
        return positionKeyframes;
    }

    public static KeyframeList<TimestampValue> getTimeKeyframes() {
        return timeKeyframes;
    }

    public static ArrayList<Keyframe> getAllKeyframes() {
        ArrayList<Keyframe> keyframeList = new ArrayList<Keyframe>();
        keyframeList.addAll(positionKeyframes);
        keyframeList.addAll(timeKeyframes);

        return keyframeList;
    }

    public static void resetKeyframes(final boolean resetMarkers, boolean callback) {
        if(getPositionKeyframes().isEmpty() && getTimeKeyframes().isEmpty()) return;

        if(!callback) {
            resetKeyframes(resetMarkers);
        } else {
            mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                @Override
                public void confirmClicked(boolean result, int id) {
                    if(result) {
                        resetKeyframes(resetMarkers);
                    }

                    mc.displayGuiScreen(null);
                }
            }, I18n.format("replaymod.gui.clearcallback.title"), I18n.format("replaymod.gui.clearcallback.message"), 1));
        }
    }

    private static void resetKeyframes(boolean resetMarkers) {
        timeKeyframes.clear();
        positionKeyframes.clear();
        selectKeyframe(null);

        if(resetMarkers) {
            markerKeyframes.clear();
        }

        setRealTimelineCursor(0);

        fireKeyframesModifyEvent();
    }

    public static void selectKeyframe(Keyframe kf) {
        selectedKeyframe = kf;
    }

    public static boolean isSelected(Keyframe kf) {
        return kf == selectedKeyframe;
    }

    public static boolean isInReplay() {
        return inReplay;
    }

    public static void startReplay(File file) {
        startReplay(file, true);
    }

    public static void startReplay(File file, boolean asyncMode) {

        ReplayMod.chatMessageHandler.initialize();
        mc.ingameGUI.getChatGUI().clearChatMessages();
        resetKeyframes(true);

        if(ReplayMod.replaySender != null) {
            ReplayMod.replaySender.terminateReplay();
        }

        if(channel != null) {
            channel.close();
        }

        setCameraTilt(0);

        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };
        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
        networkManager.setNetHandler(pc);

        channel = new EmbeddedChannel(networkManager);
        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));

        // Open replay
        try {
            currentReplayFile = new ReplayFile(file);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        KeyframeSet[] paths = currentReplayFile.paths().get();
        ReplayHandler.setKeyframeRepository(paths == null ? new KeyframeSet[0] : paths, false);

        ReplayHandler.selectKeyframe(null);

        List<Keyframe<Marker>> rawMarkerList = currentReplayFile.markers().get();
        if (rawMarkerList == null) {
            rawMarkerList = Collections.emptyList();
        }
        KeyframeList<Marker> markerList = new KeyframeList<Marker>(rawMarkerList);
        ReplayHandler.setMarkers(markerList, false);
        ReplayHandler.initialMarkers = markerList;

        PlayerVisibility visibility = currentReplayFile.visibility().get();
        PlayerHandler.loadPlayerVisibilityConfiguration(visibility);

        //load assets
        assetRepository = currentReplayFile.assetRepository().get();

        customImageObjects = new CustomObjectRepository();

        ReplayMod.replaySender = new ReplaySender(currentReplayFile, asyncMode);
        channel.pipeline().addFirst(ReplayMod.replaySender);
        channel.pipeline().fireChannelActive();

        try {
            ReplayMod.overlay.resetUI(true);
        } catch(Exception e) {
            e.printStackTrace();
            // TODO: Fix exception
        }

        //Load lighting and trigger update
        ReplayMod.replaySettings.setLightingEnabled(ReplayMod.replaySettings.isLightingEnabled());

        inReplay = true;
    }

    public static void restartReplay() {
        mc.ingameGUI.getChatGUI().clearChatMessages();

        if(channel != null) {
            channel.close();
        }

        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };

        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
        networkManager.setNetHandler(pc);

        channel = new EmbeddedChannel(networkManager);
        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));

        channel.pipeline().addFirst(ReplayMod.replaySender);
        channel.pipeline().fireChannelActive();

        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ReplayMod.overlay.resetUI(false);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        inReplay = true;
    }

    public static void endReplay() {
        if(ReplayMod.replaySender != null) {
            ReplayMod.replaySender.terminateReplay();
        }

        if (currentReplayFile != null) {
            try {

                //only if Marker keyframes changed, rewrite them
                if(!initialMarkers.equals(markerKeyframes)) {
                    File markerFile = File.createTempFile(ReplayFile.ENTRY_MARKERS, "json");
                    ReplayFileIO.write(getMarkerKeyframes(), markerFile);
                    ReplayMod.replayFileAppender.registerModifiedFile(markerFile, ReplayFile.ENTRY_MARKERS, ReplayHandler.getReplayFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    currentReplayFile.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            currentReplayFile = null;
        }

        resetKeyframes(true);

        PlayerHandler.resetHiddenPlayers();

        inReplay = false;

        FMLCommonHandler.instance().bus().post(new ReplayExitEvent());
    }

    public static void setInReplay(boolean inReplay1) {
        inReplay = inReplay1;
    }

    public static Keyframe getSelectedKeyframe() {
        return selectedKeyframe;
    }

    public static int getRealTimelineCursor() {
        return realTimelinePosition;
    }

    public static void setRealTimelineCursor(int pos) {
        realTimelinePosition = pos;
    }

    public static AdvancedPosition getLastPosition() {
        return lastPosition;
    }

    @Getter
    private static boolean forceLastPosition = false;

    public static void setLastPosition(AdvancedPosition position, boolean force) {
        lastPosition = position;
        forceLastPosition = force;
    }

    public static void moveCameraToLastPosition() {
        //get the camera position we had before jumping in time
        AdvancedPosition pos = ReplayHandler.getLastPosition();
        CameraEntity cam = ReplayHandler.getCameraEntity();
        if (cam != null && pos != null) {
            // Move camera back in case we have been respawned, unless we're more than ReplayMod.TP_DISTANCE_LIMIT away from that point
            // this is ignored if we explicitly said to respect this position, e.g. when jumping to marker keyframes.
            if (ReplayHandler.isForceLastPosition() ||
                    (Math.abs(pos.getX() - cam.posX) < ReplayMod.TP_DISTANCE_LIMIT &&
                            Math.abs(pos.getZ() - cam.posZ) < ReplayMod.TP_DISTANCE_LIMIT)) {
                cam.moveAbsolute(pos);
            }
        }
    }

    public static File getReplayFile() {
        return currentReplayFile == null ? null : currentReplayFile.getFile();
    }

    /**
     * Synchronizes the cursor on the Keyframe Timeline with the Replay Time
     * @param ignoreReplaySpeed If true, it always uses 1.0 as the stretch factor
     */
    public static void syncTimeCursor(boolean ignoreReplaySpeed) {
        selectKeyframe(null);

        int curTime = ReplayMod.replaySender.currentTimeStamp();

        int prevTime, prevRealTime;

        Keyframe<TimestampValue> keyframe = timeKeyframes.last();

        if(keyframe == null) {
            prevTime = 0;
            prevRealTime = 0;
        } else {
            prevTime = (int)keyframe.getValue().value;
            prevRealTime = keyframe.getRealTimestamp();
        }

        double speed = ignoreReplaySpeed ? 1 : ReplayMod.overlay.getSpeedSliderValue();

        int newCursorPos = Math.min(GuiReplayOverlay.KEYFRAME_TIMELINE_LENGTH, (int)(prevRealTime+((curTime-prevTime)/speed)));

        setRealTimelineCursor(newCursorPos);
    }

    public static List<CustomImageObject> getCustomImageObjects() {
        return customImageObjects.getObjects();
    }

    public static void setCustomImageObjects(List<CustomImageObject> objects) {
        customImageObjects.setObjects(new ArrayList<CustomImageObject>(objects));
    }

    public static void fireKeyframesModifyEvent() {
        FMLCommonHandler.instance().bus().post(new KeyframesModifyEvent(positionKeyframes, timeKeyframes));
        positionKeyframes.recalculate(ReplayMod.replaySettings.isLinearMovement());
        timeKeyframes.recalculate(ReplayMod.replaySettings.isLinearMovement());
    }
}
