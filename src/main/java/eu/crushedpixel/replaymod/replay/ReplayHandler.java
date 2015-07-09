package eu.crushedpixel.replaymod.replay;

import com.mojang.authlib.GameProfile;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.assets.AssetRepository;
import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.events.KeyframesModifyEvent;
import eu.crushedpixel.replaymod.events.ReplayExitEvent;
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
    private static MarkerKeyframe selectedMarkerKeyframe;

    private static boolean inPath = false;
    private static CameraEntity cameraEntity;

    private static KeyframeList<Position> positionKeyframes = new KeyframeList<Position>();
    private static KeyframeList<TimestampValue> timeKeyframes = new KeyframeList<TimestampValue>();

    private static boolean inReplay = false;
    private static Entity currentEntity = null;
    private static Position lastPosition = null;

    private static MarkerKeyframe[] initialMarkers = new MarkerKeyframe[0];
    private static List<MarkerKeyframe> markerKeyframes = new ArrayList<MarkerKeyframe>();

    private static float cameraTilt = 0;

    private static KeyframeSet[] keyframeRepository = new KeyframeSet[]{};

    @Getter @Setter
    private static AssetRepository assetRepository;

    private static List<CustomImageObject> customImageObjects = new ArrayList<CustomImageObject>();

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

    public static MarkerKeyframe[] getMarkers() {
        return markerKeyframes.toArray(new MarkerKeyframe[markerKeyframes.size()]);
    }

    public static void setMarkers(MarkerKeyframe[] m, boolean write) {
        markerKeyframes.clear();
        Collections.addAll(markerKeyframes, m);

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
        useKeyframePreset(keyframeRepository[index].getKeyframes());
    }

    public static void useKeyframePreset(Keyframe[] kfs) {
        positionKeyframes.clear();
        timeKeyframes.clear();
        for(Keyframe kf : kfs) {
            if(kf.getValue() instanceof Position) {
                positionKeyframes.add(kf);
            } else if(kf.getValue() instanceof TimestampValue) {
                timeKeyframes.add(kf);
            }
        }

        fireKeyframesModifyEvent();
    }

    public static void spectateEntity(Entity e) {
        if(e == null) {
            spectateCamera();
        }
        else {
            currentEntity = e;
            if (mc.getRenderViewEntity() != currentEntity) {
                mc.setRenderViewEntity(currentEntity);
            }
        }
    }

    public static void spectateCamera() {
        if(currentEntity != null) {
            Position prev = new Position(currentEntity, false);
            cameraEntity.movePath(prev);
        }
        currentEntity = cameraEntity;
        mc.setRenderViewEntity(cameraEntity);
    }

    public static boolean isCamera() {
        return currentEntity == cameraEntity;
    }

    public static Entity getCurrentEntity() {
        return currentEntity;
    }

    public static void startPath(RenderOptions renderOptions) {
        if(!ReplayHandler.isInPath()) {
            try {
                ReplayProcess.startReplayProcess(renderOptions);
            } catch (ReportedException e) {
                // We have to manually unwrap OOM errors as Minecraft doesn't handle them when they're wrapped
                Throwable cause = e;
                while (cause != null) {
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
        return cameraEntity;
    }

    public static void setCameraEntity(CameraEntity entity) {
        if(entity == null) return;
        if (cameraEntity != null) {
            FMLCommonHandler.instance().bus().unregister(cameraEntity);
        }
        cameraEntity = entity;
        FMLCommonHandler.instance().bus().register(cameraEntity);
        spectateCamera();
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
        if(selectedMarkerKeyframe != null) markerKeyframes.remove(selectedMarkerKeyframe);
        else {
            Position pos = new Position(mc.getRenderViewEntity(), false);
            int timestamp = ReplayMod.replaySender.currentTimeStamp();
            markerKeyframes.add(new MarkerKeyframe(timestamp, pos, null));
        }
    }

    public static void addTimeKeyframe(Keyframe<TimestampValue> keyframe) {
        timeKeyframes.add(keyframe);
        selectKeyframe(keyframe);

        fireKeyframesModifyEvent();
    }

    public static void addPositionKeyframe(Keyframe<Position> keyframe) {
        positionKeyframes.add(keyframe);
        selectKeyframe(keyframe);

        Float a = null;
        Float b;

        for(Keyframe kf : positionKeyframes) {
            if(!(kf.getValue() instanceof Position)) continue;
            Keyframe<Position> pkf = (Keyframe<Position>)kf;
            Position pos = pkf.getValue();
            b = (float)pos.getYaw() % 360;
            if(a != null) {
                float diff = b-a;
                if(Math.abs(diff) > 180) {
                    b = a - (360 - diff) % 360;
                    pos.setYaw(b);
                    pkf.setValue(pos);
                }
            }
            a = b;
        }

        fireKeyframesModifyEvent();
    }

    public static void addKeyframe(Keyframe keyframe) {
        if(keyframe.getValue() instanceof Position) {
            addPositionKeyframe(keyframe);
        } else if(keyframe.getValue() instanceof TimestampValue) {
            addTimeKeyframe(keyframe);
        }
    }

    public static void removeKeyframe(Keyframe keyframe) {
        if(keyframe.getValue() instanceof Position) {
            positionKeyframes.remove(keyframe);
        } else if(keyframe.getValue() instanceof TimestampValue) {
            timeKeyframes.remove(keyframe);
        }

        if(keyframe == selectedKeyframe) {
            selectKeyframe(null);
        }

        fireKeyframesModifyEvent();
    }

    public static MarkerKeyframe getClosestMarkerForRealTime(int realTime, int tolerance) {
        List<MarkerKeyframe> found = new ArrayList<MarkerKeyframe>();
        for(MarkerKeyframe kf : markerKeyframes) {
            if(Math.abs(kf.getRealTimestamp() - realTime) <= tolerance) {
                found.add(kf);
            }
        }

        MarkerKeyframe closest = null;

        for(MarkerKeyframe kf : found) {
            if(closest == null || Math.abs(closest.getRealTimestamp() - realTime) > Math.abs(kf.getRealTimestamp() - realTime)) {
                closest = kf;
            }
        }
        return closest;
    }

    public static MarkerKeyframe getPreviousMarkerKeyframe(int realTime) {
        if(markerKeyframes.isEmpty()) return null;
        MarkerKeyframe backup = null;
        List<MarkerKeyframe> found = new ArrayList<MarkerKeyframe>();
        for(MarkerKeyframe kf : markerKeyframes) {
            if(kf.getRealTimestamp() < realTime) {
                found.add((MarkerKeyframe)kf);
            } else if(kf.getRealTimestamp() == realTime) {
                backup = (MarkerKeyframe)kf;
            }
        }

        if(found.size() > 0)
            return found.get(found.size() - 1); //last element is nearest
        else return backup;
    }

    public static MarkerKeyframe getNextMarkerKeyframe(int realTime) {
        if(markerKeyframes.isEmpty()) return null;
        MarkerKeyframe backup = null;
        for(MarkerKeyframe kf : markerKeyframes) {
            if(kf.getRealTimestamp() > realTime) {
                return kf; //first found element is next
            } else if(kf.getRealTimestamp() == realTime) {
                backup = kf;
            }
        }
        return backup;
    }

    public static KeyframeList<Position> getPositionKeyframes() {
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

        fireKeyframesModifyEvent();
    }

    public static boolean isSelected(Keyframe kf) {
        return kf == selectedKeyframe;
    }

    public static void selectKeyframe(Keyframe kf) {
        selectedKeyframe = kf;
    }

    public static boolean isSelected(MarkerKeyframe kf) {
        return kf == selectedMarkerKeyframe;
    }

    public static void selectMarkerKeyframe(MarkerKeyframe kf) { selectedMarkerKeyframe = kf; }

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

        MarkerKeyframe[] markers = currentReplayFile.markers().get();
        if(markers == null) markers = new MarkerKeyframe[0];
        ReplayHandler.setMarkers(markers, false);
        ReplayHandler.initialMarkers = markers;

        PlayerVisibility visibility = currentReplayFile.visibility().get();
        PlayerHandler.loadPlayerVisibilityConfiguration(visibility);

        //load assets
        AssetRepository assets = currentReplayFile.assetRepository().get();
        assetRepository = assets;

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
                if(!Arrays.equals(getMarkers(), initialMarkers)) {
                    File markerFile = File.createTempFile(ReplayFile.ENTRY_MARKERS, "json");
                    ReplayFileIO.write(getMarkers(), markerFile);
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

        inReplay = false;

        FMLCommonHandler.instance().bus().post(new ReplayExitEvent());
    }

    public static void setInReplay(boolean inReplay1) {
        inReplay = inReplay1;
    }

    public static Keyframe getSelectedKeyframe() {
        return selectedKeyframe;
    }

    public static MarkerKeyframe getSelectedMarkerKeyframe() { return selectedMarkerKeyframe; }

    public static int getRealTimelineCursor() {
        return realTimelinePosition;
    }

    public static void setRealTimelineCursor(int pos) {
        realTimelinePosition = pos;
    }

    public static Position getLastPosition() {
        return lastPosition;
    }

    public static void setLastPosition(Position position) {
        lastPosition = position;
    }

    public static File getReplayFile() {
        return currentReplayFile == null ? null : currentReplayFile.getFile();
    }

    public static void syncTimeCursor(boolean shiftMode) {
        selectKeyframe(null);

        int curTime = ReplayMod.replaySender.currentTimeStamp();

        int prevTime, prevRealTime;

        Keyframe<TimestampValue> keyframe;

        //if shift is down, it will refer to the previous Time Keyframe instead of the last one
        if(shiftMode) {
            int realTime = getRealTimelineCursor();
            keyframe = timeKeyframes.getPreviousKeyframe(realTime);
        } else {
            keyframe = timeKeyframes.last();
        }

        if(keyframe == null) {
            prevTime = 0;
            prevRealTime = 0;
        } else {
            prevTime = (int)keyframe.getValue().value;
            prevRealTime = keyframe.getRealTimestamp();
        }

        int newCursorPos = prevRealTime+(curTime-prevTime);

        setRealTimelineCursor(newCursorPos);
    }

    public static List<CustomImageObject> getCustomImageObjects() {
        return customImageObjects;
    }

    public static void addCustomImageObject(CustomImageObject object) {
        customImageObjects.add(object);
    }

    public static void setCustomImageObjects(List<CustomImageObject> objects) {
        customImageObjects = objects;
    }

    public static void fireKeyframesModifyEvent() {
        FMLCommonHandler.instance().bus().post(new KeyframesModifyEvent(positionKeyframes, timeKeyframes));

    }
}
