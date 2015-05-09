package eu.crushedpixel.replaymod.replay;

import com.mojang.authlib.GameProfile;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.holders.*;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;

import java.io.File;
import java.util.*;

public class ReplayHandler {

    public static long lastExit = 0;
    private static NetworkManager networkManager;
    private static Minecraft mc = Minecraft.getMinecraft();
    //private static ReplaySender replaySender;
    private static OpenEmbeddedChannel channel;
    private static int realTimelinePosition = 0;
    private static Keyframe selectedKeyframe;
    private static boolean inPath = false;
    private static CameraEntity cameraEntity;
    private static List<Keyframe> keyframes = new ArrayList<Keyframe>();
    private static boolean inReplay = false;
    private static Entity currentEntity = null;
    private static Position lastPosition = null;

    private static float cameraTilt = 0;

    private static KeyframeSet[] keyframeRepository = new KeyframeSet[]{};

    public static KeyframeSet[] getKeyframeRepository() {
        return keyframeRepository;
    }

    public static void setKeyframeRepository(KeyframeSet[] repo, boolean write) {
        keyframeRepository = repo;
        if(write) {
            try {
                File tempFile = File.createTempFile("paths", "json");
                tempFile.deleteOnExit();

                ReplayFileIO.writeKeyframeRegistryToFile(repo, tempFile);

                ReplayMod.replayFileAppender.registerModifiedFile(tempFile, "paths", ReplayMod.replaySender.getReplayFile());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void useKeyframePresetFromRepository(int index) {
        keyframes = new ArrayList<Keyframe>(Arrays.asList(keyframeRepository[index].getKeyframes()));
    }

    public static void spectateEntity(Entity e) {
        currentEntity = e;
        mc.setRenderViewEntity(currentEntity);
    }

    public static void spectateCamera() {
        if(currentEntity != null) {
            Position prev = new Position(currentEntity);
            cameraEntity.movePath(prev);
        }
        currentEntity = cameraEntity;
        mc.setRenderViewEntity(cameraEntity);
    }

    public static boolean isCamera() {
        return currentEntity == cameraEntity;
    }

    public static void startPath(boolean save) {
        if(!ReplayHandler.isInPath()) ReplayProcess.startReplayProcess(save);
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
        cameraEntity = entity;
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

    public static void sortKeyframes() {
        Collections.sort(keyframes, new KeyframeComparator());
    }

    public static void addKeyframe(Keyframe keyframe) {
        keyframes.add(keyframe);
        selectKeyframe(keyframe);


        if(keyframe instanceof PositionKeyframe) {
            Float a = null;
            Float b;

            for(Keyframe kf : keyframes) {
                if(!(kf instanceof PositionKeyframe)) continue;
                PositionKeyframe pkf = (PositionKeyframe)kf;
                Position pos = pkf.getPosition();
                b = pos.getYaw() % 360;
                System.out.println(a+" "+b);
                if(a != null) {
                    float diff = b-a;
                    if(Math.abs(diff) > 180) {
                        b = a - (360 - diff) % 360;
                        System.out.println(b);
                        pos.setYaw(b);
                        pkf.setPosition(pos);
                    }
                }
                a = b;
            }
        }
    }

    public static void removeKeyframe(Keyframe keyframe) {
        keyframes.remove(keyframe);
        if(keyframe == selectedKeyframe) {
            selectKeyframe(null);
        } else {
            sortKeyframes();
        }
    }

    public static int getKeyframeIndex(TimeKeyframe timeKeyframe) {
        int index = 0;
        for(Keyframe kf : keyframes) {
            if(kf == timeKeyframe) return index;
            else if(kf instanceof TimeKeyframe) index++;
        }
        return -1;
    }

    public static int getKeyframeIndex(PositionKeyframe posKeyframe) {
        int index = 0;
        for(Keyframe kf : keyframes) {
            if(kf == posKeyframe) return index;
            else if(kf instanceof PositionKeyframe) index++;
        }
        return -1;
    }

    public static int getPosKeyframeCount() {
        int size = 0;
        for(Keyframe kf : keyframes) {
            if(kf instanceof PositionKeyframe) size++;
        }
        return size;
    }

    public static int getTimeKeyframeCount() {
        int size = 0;
        for(Keyframe kf : keyframes) {
            if(kf instanceof TimeKeyframe) size++;
        }
        return size;
    }

    public static TimeKeyframe getClosestTimeKeyframeForRealTime(int realTime, int tolerance) {
        List<TimeKeyframe> found = new ArrayList<TimeKeyframe>();
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof TimeKeyframe)) continue;
            if(Math.abs(kf.getRealTimestamp() - realTime) <= tolerance) {
                found.add((TimeKeyframe) kf);
            }
        }

        TimeKeyframe closest = null;

        for(TimeKeyframe kf : found) {
            if(closest == null || Math.abs(closest.getTimestamp() - realTime) > Math.abs(kf.getRealTimestamp() - realTime)) {
                closest = kf;
            }
        }
        return closest;
    }

    public static PositionKeyframe getClosestPlaceKeyframeForRealTime(int realTime, int tolerance) {
        List<PositionKeyframe> found = new ArrayList<PositionKeyframe>();
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof PositionKeyframe)) continue;
            if(Math.abs(kf.getRealTimestamp() - realTime) <= tolerance) {
                found.add((PositionKeyframe) kf);
            }
        }

        PositionKeyframe closest = null;

        for(PositionKeyframe kf : found) {
            if(closest == null || Math.abs(closest.getRealTimestamp() - realTime) > Math.abs(kf.getRealTimestamp() - realTime)) {
                closest = kf;
            }
        }
        return closest;
    }

    public static PositionKeyframe getPreviousPositionKeyframe(int realTime) {
        if(keyframes.isEmpty()) return null;
        List<PositionKeyframe> found = new ArrayList<PositionKeyframe>();
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof PositionKeyframe)) continue;
            if(kf.getRealTimestamp() < realTime) {
                found.add((PositionKeyframe) kf);
            }
        }

        if(found.size() > 0)
            return found.get(found.size() - 1); //last element is nearest
        else return null;
    }

    public static PositionKeyframe getNextPositionKeyframe(int realTime) {
        if(keyframes.isEmpty()) return null;
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof PositionKeyframe)) continue;
            if(kf.getRealTimestamp() >= realTime) {
                return (PositionKeyframe) kf; //first found element is next
            }
        }
        return null;
    }

    public static TimeKeyframe getPreviousTimeKeyframe(int realTime) {
        if(keyframes.isEmpty()) return null;
        List<TimeKeyframe> found = new ArrayList<TimeKeyframe>();
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof TimeKeyframe)) continue;
            if(kf.getRealTimestamp() < realTime) {
                found.add((TimeKeyframe) kf);
            }
        }

        if(found.size() > 0)
            return found.get(found.size() - 1); //last element is nearest
        else return null;
    }

    public static TimeKeyframe getNextTimeKeyframe(int realTime) {
        if(keyframes.isEmpty()) return null;
        for(Keyframe kf : keyframes) {
            if(!(kf instanceof TimeKeyframe)) continue;
            if(kf.getRealTimestamp() >= realTime) {
                return (TimeKeyframe) kf; //first found element is next
            }
        }
        return null;
    }

    public static List<Keyframe> getKeyframes() {
        return new ArrayList<Keyframe>(keyframes);
    }

    public static void resetKeyframes() {
        keyframes = new ArrayList<Keyframe>();

        selectKeyframe(null);
    }

    public static boolean isSelected(Keyframe kf) {
        return kf == selectedKeyframe;
    }

    public static void selectKeyframe(Keyframe kf) {
        selectedKeyframe = kf;
        sortKeyframes();
    }

    public static boolean isInReplay() {
        return inReplay;
    }

    public static void startReplay(File file) throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        ReplayMod.chatMessageHandler.initialize();
        mc.ingameGUI.getChatGUI().clearChatMessages();
        resetKeyframes();

        if(ReplayMod.replaySender != null) {
            ReplayMod.replaySender.terminateReplay();
        }

        if(channel != null) {
            channel.close();
        }

        setCameraTilt(0);

        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
        networkManager.setNetHandler(pc);

        channel = new OpenEmbeddedChannel(networkManager);

        ReplayMod.replaySender = new ReplaySender(file, networkManager);
        channel.pipeline().addFirst(ReplayMod.replaySender);
        channel.pipeline().fireChannelActive();

        try {
            ReplayMod.overlay.resetUI(true);
        } catch(Exception e) {}

        //Load lighting and trigger update
        ReplayMod.replaySettings.setLightingEnabled(ReplayMod.replaySettings.isLightingEnabled());

        inReplay = true;
    }

    public static void restartReplay() {
        mc.ingameGUI.getChatGUI().clearChatMessages();

        if(channel != null) {
            channel.close();
        }

        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
        networkManager.setNetHandler(pc);

        EmbeddedChannel channel = new OpenEmbeddedChannel(networkManager);

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

        resetKeyframes();

        inReplay = false;
    }

    public static Keyframe getSelected() {
        return selectedKeyframe;
    }

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
        if(ReplayMod.replaySender != null) {
            return ReplayMod.replaySender.getReplayFile();
        }
        return null;
    }

    public static TimeKeyframe getFirstTimeKeyframe() {
        Keyframe sel = getSelected();
        sortKeyframes();
        for(Keyframe k : getKeyframes()) {
            if(k instanceof TimeKeyframe) {
                selectKeyframe(sel);
                return (TimeKeyframe)k;
            }
        }
        selectKeyframe(sel);
        return null;
    }

    public static PositionKeyframe getFirstPositionKeyframe() {
        Keyframe sel = getSelected();
        sortKeyframes();
        for(Keyframe k : getKeyframes()) {
            if(k instanceof PositionKeyframe) {
                selectKeyframe(sel);
                return (PositionKeyframe)k;
            }
        }
        selectKeyframe(sel);
        return null;
    }

    public static TimeKeyframe getLastTimeKeyframe() {
        ArrayList<Keyframe> rev = new ArrayList<Keyframe>(getKeyframes());
        Collections.reverse(rev);

        for(Keyframe k : rev) {
            if(k instanceof TimeKeyframe) {
                return (TimeKeyframe)k;
            }
        }
        return null;
    }

    public static PositionKeyframe getLastPositionKeyframe() {
        ArrayList<Keyframe> rev = new ArrayList<Keyframe>(getKeyframes());
        Collections.reverse(rev);

        for(Keyframe k : rev) {
            if(k instanceof PositionKeyframe) {
                return (PositionKeyframe)k;
            }
        }
        return null;
    }

    public static void syncTimeCursor(boolean shiftMode) {
        selectKeyframe(null);

        int curTime = ReplayMod.replaySender.currentTimeStamp();

        int prevTime, prevRealTime;

        TimeKeyframe keyframe;

        //if shift is down, it will refer to the previous Time Keyframe instead of the last one
        if(shiftMode) {
            int realTime = getRealTimelineCursor();
            keyframe = getPreviousTimeKeyframe(realTime);
        } else {
            keyframe = getLastTimeKeyframe();
        }

        if(keyframe == null) {
            prevTime = 0;
            prevRealTime = 0;
        } else {
            prevTime = keyframe.getTimestamp();
            prevRealTime = keyframe.getRealTimestamp();
        }

        int newCursorPos = prevRealTime+(curTime-prevTime);

        setRealTimelineCursor(newCursorPos);
    }
}
