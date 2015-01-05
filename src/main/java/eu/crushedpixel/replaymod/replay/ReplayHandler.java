package eu.crushedpixel.replaymod.replay;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayClient;

import com.mojang.authlib.GameProfile;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeComparator;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;

public class ReplayHandler {

	private static NetworkManager networkManager;
	private static Minecraft mc = Minecraft.getMinecraft();
	private static ReplaySender replaySender;
	private static OpenEmbeddedChannel channel;

	private static Keyframe selectedKeyframe;

	private static boolean isReplaying = false;

	private static CameraEntity cameraEntity;

	private static List<Keyframe> keyframes = new ArrayList<Keyframe>();

	private static boolean replayActive = false;

	public static long lastExit = 0;

	public static void setReplaying(boolean replaying) {
		isReplaying = replaying;
	}

	public static void startPath() {
		ReplayProcess.startReplayProcess();
	}

	public static void interruptReplay() {
		ReplayProcess.stopReplayProcess(false);
	}

	public static boolean isReplaying() {
		return isReplaying;
	}
	
	public static void setCameraEntity(CameraEntity entity) {
		if(entity == null) return;
		cameraEntity = entity;
		mc.setRenderViewEntity(cameraEntity);
	}

	public static CameraEntity getCameraEntity() {
		return cameraEntity;
	}

	public static int getReplayTime() {
		if(replaySender != null) {
			return (int)replaySender.currentTimeStamp();
		}

		return 0;
	}

	public static void sortKeyframes() {
		Collections.sort(keyframes, new KeyframeComparator());
	}

	public static void addKeyframe(Keyframe keyframe) {
		keyframes.add(keyframe);
		selectKeyframe(keyframe);
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
			if(Math.abs(kf.getRealTimestamp()-realTime) <= tolerance) {
				found.add((TimeKeyframe)kf);
			}
		}

		TimeKeyframe closest = null;

		for(TimeKeyframe kf : found) {
			if(closest == null || Math.abs(closest.getTimestamp()-realTime) > Math.abs(kf.getRealTimestamp()-realTime)) {
				closest = kf;
			}
		}
		return closest;
	}

	public static PositionKeyframe getClosestPlaceKeyframeForRealTime(int realTime, int tolerance) {
		List<PositionKeyframe> found = new ArrayList<PositionKeyframe>();
		for(Keyframe kf : keyframes) {
			if(!(kf instanceof PositionKeyframe)) continue;
			if(Math.abs(kf.getRealTimestamp()-realTime) <= tolerance) {
				found.add((PositionKeyframe)kf);
			}
		}

		PositionKeyframe closest = null;

		for(PositionKeyframe kf : found) {
			if(closest == null || Math.abs(closest.getRealTimestamp()-realTime) > Math.abs(kf.getRealTimestamp()-realTime)) {
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
				found.add((PositionKeyframe)kf);
			}
		}

		if(found.size() > 0)
			return found.get(found.size()-1); //last element is nearest
		else return null;
	}

	public static PositionKeyframe getNextPositionKeyframe(int realTime) {
		if(keyframes.isEmpty()) return null;
		for(Keyframe kf : keyframes) {
			if(!(kf instanceof PositionKeyframe)) continue;
			if(kf.getRealTimestamp() >= realTime) {
				return (PositionKeyframe)kf; //first found element is next
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
				found.add((TimeKeyframe)kf);
			}
		}

		if(found.size() > 0)
			return found.get(found.size()-1); //last element is nearest
		else return null;
	}

	public static TimeKeyframe getNextTimeKeyframe(int realTime) {
		if(keyframes.isEmpty()) return null;
		for(Keyframe kf : keyframes) {
			if(!(kf instanceof TimeKeyframe)) continue;
			if(kf.getRealTimestamp() >= realTime) {
				return (TimeKeyframe)kf; //first found element is next
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

	public static void setReplayPos(int pos, boolean force) {
		if(replaySender != null) {
			replaySender.jumpToTime(pos, force);
		}
	}
	
	public static boolean isHurrying() {
		if(replaySender != null) {
			return replaySender.isHurrying();
		}
		return false;
	}

	public static int getReplayLength() {
		if(replaySender != null) {
			return replaySender.replayLength();
		}

		return 1;
	}

	public static boolean isSelected(Keyframe kf) {
		return kf == selectedKeyframe;
	}

	public static void selectKeyframe(Keyframe kf) {
		selectedKeyframe = kf;
		sortKeyframes();
	}

	public static boolean replayActive() {
		return replayActive;
	}

	public static boolean isPaused() {
		if(replaySender != null) {
			return replaySender.paused();
		}
		return true;
	}

	public static void setSpeed(double d) {
		if(replaySender != null) {
			replaySender.setReplaySpeed(d);
		}
	}

	public static double getSpeed() {
		if(replaySender != null) {
			return replaySender.getReplaySpeed();
		}
		return 0;
	}

	public static void startReplay(File file) throws NoSuchMethodException, SecurityException, NoSuchFieldException {
		
		ChatMessageRequests.initialize();
		mc.ingameGUI.getChatGUI().clearChatMessages();
		resetKeyframes();

		if(replaySender != null) {
			replaySender.terminateReplay();
		}

		if(channel != null) {
			channel.close();
		}

		networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
		INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, (GuiScreen)null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
		networkManager.setNetHandler(pc);

		channel = new OpenEmbeddedChannel(networkManager);

		replaySender = new ReplaySender(file, networkManager);
		channel.pipeline().addFirst(replaySender);
		channel.pipeline().fireChannelActive();

		try {
			ReplayMod.overlay.resetUI();
		} catch(Exception e) {}

		replayActive = true;
	}

	public static void restartReplay() {
		//mc.setRenderViewEntity(mc.thePlayer);
		mc.ingameGUI.getChatGUI().clearChatMessages();

		if(channel != null) {
			channel.close();
		}

		networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
		INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, (GuiScreen)null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
		networkManager.setNetHandler(pc);

		EmbeddedChannel channel = new OpenEmbeddedChannel(networkManager);

		channel.pipeline().addFirst(replaySender);
		channel.pipeline().fireChannelActive();

		ChannelPipeline pipeline = networkManager.channel().pipeline();

		try {
			ReplayMod.overlay.resetUI();
		} catch(Exception e) {}

		replayActive = true;
	}

	public static void endReplay() {
		if(replaySender != null) {
			replaySender.terminateReplay();
		}

		resetKeyframes();

		if(channel != null) {
			channel.close();
		}

		replayActive = false;
	}

	public static Keyframe getSelected() {
		return selectedKeyframe;
	}
}
