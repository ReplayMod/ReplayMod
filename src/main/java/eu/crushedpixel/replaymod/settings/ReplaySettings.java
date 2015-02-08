package eu.crushedpixel.replaymod.settings;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.util.Timer;
import net.minecraftforge.common.config.Property;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class ReplaySettings {

	private boolean enableRecordingServer = true;
	private boolean enableRecordingSingleplayer = true;
	private boolean showNotifications = true;
	private boolean forceLinearPath = false;
	private boolean lightingEnabled = false;
	private float videoQuality = 0.5f;
	private int videoFramerate = 30;

	private static Field mcTimer;

	static {
		try {
			mcTimer = Minecraft.class.getDeclaredField(MCPNames.field("field_71428_T"));
			mcTimer.setAccessible(true);
		} catch(Exception e) {
			mcTimer = null;
			e.printStackTrace();
		}
	}

	public ReplaySettings(boolean enableRecordingServer,
			boolean enableRecordingSingleplayer, boolean showNotifications, boolean forceLinearPath, boolean lightingEnabled, int framerate, float videoQuality) {
		this.enableRecordingServer = enableRecordingServer;
		this.enableRecordingSingleplayer = enableRecordingSingleplayer;
		this.showNotifications = showNotifications;
		this.forceLinearPath = forceLinearPath;
		this.lightingEnabled = lightingEnabled;
		this.videoFramerate = Math.min(120, Math.max(10, framerate));
		this.videoQuality = Math.min(0.9f, Math.max(0.1f, videoQuality));
	}

	public int getVideoFramerate() {
		return videoFramerate;
	}
	public void setVideoFramerate(int framerate) {
		this.videoFramerate = Math.min(120, Math.max(10, framerate));
		rewriteSettings();
	}
	public float getVideoQuality() {
		return videoQuality;
	}
	public void setVideoQuality(float videoQuality) {
		this.videoQuality = Math.min(0.9f, Math.max(0.1f, videoQuality));
		rewriteSettings();
	}
	public boolean isEnableRecordingServer() {
		return enableRecordingServer;
	}
	public void setEnableRecordingServer(boolean enableRecordingServer) {
		this.enableRecordingServer = enableRecordingServer;
		rewriteSettings();
	}
	public boolean isEnableRecordingSingleplayer() {
		return enableRecordingSingleplayer;
	}
	public void setEnableRecordingSingleplayer(boolean enableRecordingSingleplayer) {
		this.enableRecordingSingleplayer = enableRecordingSingleplayer;
		rewriteSettings();
	}
	public boolean isShowNotifications() {
		return showNotifications;
	}
	public void setShowNotifications(boolean showNotifications) {
		this.showNotifications = showNotifications;
		rewriteSettings();
	}
	public boolean isLinearMovement() {
		return forceLinearPath;
	}
	public void setLinearMovement(boolean linear) {
		this.forceLinearPath = linear;
		rewriteSettings();
	}
	public boolean isLightingEnabled() {
		return lightingEnabled;
	}
	public void setLightingEnabled(boolean enabled) {
		this.lightingEnabled = enabled;
		if(enabled) {
			Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, 1000);
		} else {
			Minecraft.getMinecraft().gameSettings.setOptionFloatValue(Options.GAMMA, ReplayHandler.getInitialGamma());
		}
		try {
			if(ReplayHandler.isPaused()) {
				Timer timer = (Timer)mcTimer.get(Minecraft.getMinecraft());
				timer.elapsedPartialTicks++;
				timer.renderPartialTicks++;
			} else {
				Minecraft.getMinecraft().entityRenderer.updateCameraAndRender(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		rewriteSettings();
	}


	public Map<String, Object> getOptions() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put("Enable Notifications", showNotifications);
		map.put("Record Server", enableRecordingServer);
		map.put("Record Singleplayer", enableRecordingSingleplayer);
		map.put("Placeholder 1", null);
		map.put("Force Linear Movement", forceLinearPath);
		map.put("Enable Lighting", lightingEnabled);
		map.put("Video Quality", videoQuality);
		map.put("Video Framerate", videoFramerate);

		return map;
	}

	public void setOptions(Map<String, Object> map) {
		try {

			showNotifications = (Boolean)map.get("Enable Notifications");
			enableRecordingServer = (Boolean)map.get("Record Server");
			enableRecordingSingleplayer = (Boolean)map.get("Record Singleplayer");
			forceLinearPath = (Boolean)map.get("Force Linear Movement");
			lightingEnabled = (Boolean)map.get("Enable Lighting");
			videoQuality = (Float)map.get("Video Quality");
			videoFramerate = (Integer)map.get("Video Framerate");

			rewriteSettings();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void rewriteSettings() {
		ReplayMod.instance.config.load();

		ReplayMod.instance.config.removeCategory(ReplayMod.instance.config.getCategory("settings"));
		Property recServer = ReplayMod.instance.config.get("settings", "enableRecordingServer", enableRecordingServer, "Defines whether a recording should be started upon joining a server.");
		Property recSP = ReplayMod.instance.config.get("settings", "enableRecordingSingleplayer", enableRecordingSingleplayer, "Defines whether a recording should be started upon joining a singleplayer world.");
		Property showNot = ReplayMod.instance.config.get("settings", "showNotifications", showNotifications, "Defines whether notifications should be sent to the player.");
		Property linear = ReplayMod.instance.config.get("settings", "forceLinearPath", forceLinearPath, "Defines whether travelling paths should be linear instead of interpolated.");
		Property lighting = ReplayMod.instance.config.get("settings", "enableLighting", lightingEnabled, "If enabled, the whole map is lighted.");
		Property vq = ReplayMod.instance.config.get("settings", "videoQuality", videoQuality, "The quality of the exported video files from 0.1 to 0.9");
		Property framerate = ReplayMod.instance.config.get("settings", "videoFramerate", videoFramerate, "The framerate of the exported video files from 10 to 120");
		
		ReplayMod.instance.config.save();
	}
}
