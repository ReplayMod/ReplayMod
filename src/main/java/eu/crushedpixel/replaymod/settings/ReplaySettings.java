package eu.crushedpixel.replaymod.settings;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.util.Timer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class ReplaySettings {

	public enum Option {
		recordServer(true), recordSingleplayer(true), notifications(true), linear(false), 
		lighting(false), useResources(true), videoQuality(0.5f), videoFramerate(30);
		
		private Object value;
		
		public Object getValue() {
			return value;
		}
		
		public void setValue(Object value) {
			this.value = value;
		}
		
		Option(Object value) {
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
	
	public void readValues() {
		Configuration config = ReplayMod.config;
		
		for(Option o : Option.values()) {
			Property p = getConfigSetting(config, o);
			o.setValue(getValueObject(p));
		}
		
		config.save();
	}
	
	public int getVideoFramerate() {
		return (Integer)Option.videoFramerate.getValue();
	}
	public void setVideoFramerate(int framerate) {
		Option.videoFramerate.setValue(Math.min(120, Math.max(10, framerate)));
		rewriteSettings();
	}
	public double getVideoQuality() {
		return (Double)Option.videoQuality.getValue();
	}
	public void setVideoQuality(double videoQuality) {
		Option.videoQuality.setValue(Math.min(0.9f, Math.max(0.1f, videoQuality)));
		rewriteSettings();
	}
	public boolean isEnableRecordingServer() {
		return (Boolean)Option.recordServer.getValue();
	}
	public void setEnableRecordingServer(boolean enableRecordingServer) {
		Option.recordServer.setValue(enableRecordingServer);
		rewriteSettings();
	}
	public boolean isEnableRecordingSingleplayer() {
		return (Boolean)Option.recordSingleplayer.getValue();
	}
	public void setEnableRecordingSingleplayer(boolean enableRecordingSingleplayer) {
		Option.recordSingleplayer.setValue(enableRecordingSingleplayer);
		rewriteSettings();
	}
	public boolean isShowNotifications() {
		return (Boolean)Option.notifications.getValue();
	}
	public void setShowNotifications(boolean showNotifications) {
		Option.notifications.setValue(showNotifications);
		rewriteSettings();
	}
	public boolean isLinearMovement() {
		return (Boolean)Option.linear.getValue();
	}
	public void setLinearMovement(boolean linear) {
		Option.linear.setValue(linear);
		rewriteSettings();
	}
	public boolean isLightingEnabled() {
		return (Boolean)Option.lighting.getValue();
	}
	public void setUseResourcePacks(boolean use) {
		Option.useResources.setValue(use);
		rewriteSettings();
	}
	public boolean getUseResourcePacks() {
		return (Boolean)Option.useResources.getValue();
	}
	
	//TODO: FIX
	public void setLightingEnabled(boolean enabled) {
		Option.lighting.setValue(enabled);
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
		ReplayMod.instance.config.removeCategory(ReplayMod.instance.config.getCategory("settings"));
		
		for(Option o : Option.values()) {
			getConfigSetting(ReplayMod.instance.config, o);
		}
		
		ReplayMod.instance.config.save();
	}
	
	private Property getConfigSetting(Configuration config, Option o) {
		Object value = o.getValue();
		if(value instanceof Integer) {
			return config.get("settings", o.name(), (Integer)o.getValue());
		} else if(value instanceof Boolean) {
			return config.get("settings", o.name(), (Boolean)o.getValue());
		} else if(value instanceof Double) {
			return config.get("settings", o.name(), (Double)o.getValue());
		} else if(value instanceof Float) {
			return config.get("settings", o.name(), (double)(Float)o.getValue());
		} else if(value instanceof String) {
			return config.get("settings", o.name(), (String)o.getValue());
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
