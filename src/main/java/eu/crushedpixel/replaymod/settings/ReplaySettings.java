package eu.crushedpixel.replaymod.settings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.util.Timer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class ReplaySettings {

	public static interface ValueEnum {
		public Object getValue();
		public void setValue(Object value);
	}

	public enum RecordingOptions implements ValueEnum {
		recordServer(true), recordSingleplayer(true), notifications(true), indicator(true);

		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		RecordingOptions(Object value) {
			this.value = value;
		}
	}

	public enum ReplayOptions implements ValueEnum {
		linear(false), lighting(false), useResources(true);

		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		ReplayOptions(Object value) {
			this.value = value;
		}
	}

	public enum RenderOptions implements ValueEnum {
		videoQuality(0.5f), videoFramerate(30), waitForChunks(true);

		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		RenderOptions(Object value) {
			this.value = value;
		}
	}

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

	public List<ValueEnum> getValueEnums() {
		List<ValueEnum> enums = new ArrayList<ReplaySettings.ValueEnum>();
		enums.addAll(Arrays.asList(ReplayOptions.values()));
		enums.addAll(Arrays.asList(RenderOptions.values()));
		return enums;
	}

	public void readValues() {
		Configuration config = ReplayMod.config;

		for(RecordingOptions o : RecordingOptions.values()) {
			Property p = getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "recording");
			o.setValue(getValueObject(p));
		}
		
		for(ReplayOptions o : ReplayOptions.values()) {
			Property p = getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "replay");
			o.setValue(getValueObject(p));
		}

		for(RenderOptions o : RenderOptions.values()) {
			Property p = getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "render");
			o.setValue(getValueObject(p));
		}

		config.save();
	}

	public int getVideoFramerate() {
		return (Integer)RenderOptions.videoFramerate.getValue();
	}
	public void setVideoFramerate(int framerate) {
		RenderOptions.videoFramerate.setValue(Math.min(120, Math.max(10, framerate)));
		rewriteSettings();
	}
	public double getVideoQuality() {
		return (Double)RenderOptions.videoQuality.getValue();
	}
	public void setEnableIndicator(boolean enable) {
		RecordingOptions.indicator.setValue(enable);
		rewriteSettings();
	}
	public boolean showRecordingIndicator() {
		return (Boolean)RecordingOptions.indicator.getValue();
	}
	public void setVideoQuality(double videoQuality) {
		RenderOptions.videoQuality.setValue(Math.min(0.9f, Math.max(0.1f, videoQuality)));
		rewriteSettings();
	}
	public boolean isEnableRecordingServer() {
		return (Boolean)RecordingOptions.recordServer.getValue();
	}
	public void setEnableRecordingServer(boolean enableRecordingServer) {
		RecordingOptions.recordServer.setValue(enableRecordingServer);
		rewriteSettings();
	}
	public boolean isEnableRecordingSingleplayer() {
		return (Boolean)RecordingOptions.recordSingleplayer.getValue();
	}
	public void setEnableRecordingSingleplayer(boolean enableRecordingSingleplayer) {
		RecordingOptions.recordSingleplayer.setValue(enableRecordingSingleplayer);
		rewriteSettings();
	}
	public boolean isShowNotifications() {
		return (Boolean)RecordingOptions.notifications.getValue();
	}
	public void setShowNotifications(boolean showNotifications) {
		RecordingOptions.notifications.setValue(showNotifications);
		rewriteSettings();
	}
	public boolean isLinearMovement() {
		return (Boolean)ReplayOptions.linear.getValue();
	}
	public void setLinearMovement(boolean linear) {
		ReplayOptions.linear.setValue(linear);
		rewriteSettings();
	}
	public boolean isLightingEnabled() {
		return (Boolean)ReplayOptions.lighting.getValue();
	}
	public void setUseResourcePacks(boolean use) {
		ReplayOptions.useResources.setValue(use);
		rewriteSettings();
	}
	public boolean getUseResourcePacks() {
		return (Boolean)ReplayOptions.useResources.getValue();
	}
	public void setWaitForChunks(boolean wait) {
		RenderOptions.waitForChunks.setValue(wait);
		rewriteSettings();
	}
	public boolean getWaitForChunks() {
		return (Boolean)RenderOptions.waitForChunks.getValue();
	}

	//TODO: FIX
	public void setLightingEnabled(boolean enabled) {
		ReplayOptions.lighting.setValue(enabled);
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

	public void rewriteSettings() {
		ReplayMod.instance.config.load();

		for(String cat : ReplayMod.instance.config.getCategoryNames()) {
			ReplayMod.instance.config.removeCategory(ReplayMod.instance.config.getCategory(cat));
		}

		for(ReplayOptions o : ReplayOptions.values()) {
			getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "replay");
		}

		for(RenderOptions o : RenderOptions.values()) {
			getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "render");
		}

		ReplayMod.instance.config.save();
	}

	private Property getConfigSetting(Configuration config, String name, Object value, String category) {
		if(value instanceof Integer) {
			return config.get(category, name, (Integer)value);
		} else if(value instanceof Boolean) {
			return config.get(category, name, (Boolean)value);
		} else if(value instanceof Double) {
			return config.get(category, name, (Double)value);
		} else if(value instanceof Float) {
			return config.get(category, name, (double)(Float)value);
		} else if(value instanceof String) {
			return config.get(category, name, (String)value);
		}
		return null;
	}
	private Object getValueObject(Property p) {
		if(p.isIntValue()) {
			return p.getInt();
		} else if(p.isDoubleValue()) {
			return p.getDouble();
		} else if(p.isBooleanValue()) {
			return p.getBoolean();
		}
		return null;
	}
}
