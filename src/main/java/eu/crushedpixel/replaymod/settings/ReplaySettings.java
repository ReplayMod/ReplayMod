package eu.crushedpixel.replaymod.settings;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.registry.LightingHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplaySettings {

    public List<ValueEnum> getValueEnums() {
        List<ValueEnum> enums = new ArrayList<ReplaySettings.ValueEnum>();
        enums.addAll(Arrays.asList(ReplayOptions.values()));
        enums.addAll(Arrays.asList(RenderOptions.values()));
        return enums;
    }

    public void readValues() {
        Configuration config = ReplayMod.config;

        for(RecordingOptions o : RecordingOptions.values()) {
            Property p = getConfigSetting(config, o.name(), o.getValue(), "recording", false);
            o.setValue(getValueObject(p));
        }

        for(ReplayOptions o : ReplayOptions.values()) {
            Property p = getConfigSetting(config, o.name(), o.getValue(), "replay", false);
            o.setValue(getValueObject(p));
        }

        for(RenderOptions o : RenderOptions.values()) {
            Property p = getConfigSetting(config, o.name(), o.getValue(), "render", false);
            o.setValue(getValueObject(p));
        }

        for(AdvancedOptions o : AdvancedOptions.values()) {
            Property p = getConfigSetting(config, o.name(), o.getValue(), "advanced", true);
            o.setValue(getValueObject(p));
        }

        config.save();
    }

    public String getRecordingPath() {
        return (String) AdvancedOptions.recordingPath.getValue();
    }

    public String getRenderPath() {
        return (String) AdvancedOptions.renderPath.getValue();
    }

    public int getVideoFramerate() {
        return (Integer) RenderOptions.videoFramerate.getValue();
    }

    public void setVideoFramerate(int framerate) {
        RenderOptions.videoFramerate.setValue(Math.min(120, Math.max(10, framerate)));
        rewriteSettings();
    }

    public double getVideoQuality() {
        return (Double) RenderOptions.videoQuality.getValue();
    }

    public void setVideoQuality(double videoQuality) {
        RenderOptions.videoQuality.setValue(Math.min(0.9f, Math.max(0.1f, videoQuality)));
        rewriteSettings();
    }

    public void setEnableIndicator(boolean enable) {
        RecordingOptions.indicator.setValue(enable);
        rewriteSettings();
    }

    public boolean showRecordingIndicator() {
        return (Boolean) RecordingOptions.indicator.getValue();
    }

    public boolean isEnableRecordingServer() {
        return (Boolean) RecordingOptions.recordServer.getValue();
    }

    public void setEnableRecordingServer(boolean enableRecordingServer) {
        RecordingOptions.recordServer.setValue(enableRecordingServer);
        rewriteSettings();
    }

    public boolean isEnableRecordingSingleplayer() {
        return (Boolean) RecordingOptions.recordSingleplayer.getValue();
    }

    public void setEnableRecordingSingleplayer(boolean enableRecordingSingleplayer) {
        RecordingOptions.recordSingleplayer.setValue(enableRecordingSingleplayer);
        rewriteSettings();
    }

    public boolean isShowNotifications() {
        return (Boolean) RecordingOptions.notifications.getValue();
    }

    public void setShowNotifications(boolean showNotifications) {
        RecordingOptions.notifications.setValue(showNotifications);
        rewriteSettings();
    }

    public boolean isLinearMovement() {
        return (Boolean) ReplayOptions.linear.getValue();
    }

    public void setLinearMovement(boolean linear) {
        ReplayOptions.linear.setValue(linear);
        rewriteSettings();
    }

    public boolean isLightingEnabled() {
        return (Boolean) ReplayOptions.lighting.getValue();
    }

    public void setLightingEnabled(boolean enabled) {
        ReplayOptions.lighting.setValue(enabled);
        LightingHandler.setLighting(enabled);
        rewriteSettings();
    }

    public boolean getUseResourcePacks() {
        return (Boolean) ReplayOptions.useResources.getValue();
    }

    public void setUseResourcePacks(boolean use) {
        ReplayOptions.useResources.setValue(use);
        rewriteSettings();
    }

    public boolean getWaitForChunks() {
        return (Boolean) RenderOptions.waitForChunks.getValue();
    }

    public void setWaitForChunks(boolean wait) {
        RenderOptions.waitForChunks.setValue(wait);
        rewriteSettings();
    }

    public void rewriteSettings() {
        ReplayMod.instance.config.load();

        for(String cat : ReplayMod.instance.config.getCategoryNames()) {
            ReplayMod.instance.config.removeCategory(ReplayMod.instance.config.getCategory(cat));
        }

        for(RecordingOptions o : RecordingOptions.values()) {
            getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "recording", false);
        }

        for(ReplayOptions o : ReplayOptions.values()) {
            getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "replay", false);
        }

        for(RenderOptions o : RenderOptions.values()) {
            getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "render", false);
        }

        for(AdvancedOptions o : AdvancedOptions.values()) {
            getConfigSetting(ReplayMod.instance.config, o.name(), o.getValue(), "advanced", false);
        }

        ReplayMod.instance.config.save();
    }

    private Property getConfigSetting(Configuration config, String name, Object value, String category, boolean warning) {
        if(warning) {
            String warningMsg = "Please be careful when modifying this setting, as setting it to an invalid value might harm your computer.";
            if(value instanceof Integer) {
                return config.get(category, name, (Integer) value, warningMsg);
            } else if(value instanceof Boolean) {
                return config.get(category, name, (Boolean) value, warningMsg);
            } else if(value instanceof Double) {
                return config.get(category, name, (Double) value, warningMsg);
            } else if(value instanceof Float) {
                return config.get(category, name, (double) (Float) value, warningMsg);
            } else if(value instanceof String) {
                return config.get(category, name, (String) value, warningMsg);
            }
        } else {
            if(value instanceof Integer) {
                return config.get(category, name, (Integer) value);
            } else if(value instanceof Boolean) {
                return config.get(category, name, (Boolean) value);
            } else if(value instanceof Double) {
                return config.get(category, name, (Double) value);
            } else if(value instanceof Float) {
                return config.get(category, name, (double) (Float) value);
            } else if(value instanceof String) {
                return config.get(category, name, (String) value);
            }
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
        } else {
            return p.getString();
        }
    }

    public enum RecordingOptions implements ValueEnum {
        recordServer(true), recordSingleplayer(true), notifications(true), indicator(true);

        private Object value;

        RecordingOptions(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public enum ReplayOptions implements ValueEnum {
        linear(false), lighting(false), useResources(true);

        private Object value;

        ReplayOptions(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public enum RenderOptions implements ValueEnum {
        videoQuality(0.5f), videoFramerate(30), waitForChunks(true);

        private Object value;

        RenderOptions(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public enum AdvancedOptions implements ValueEnum {
        recordingPath("./replay_recordings/"), renderPath("./replay_videos/");

        private Object value;

        AdvancedOptions(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static interface ValueEnum {
        public Object getValue();

        public void setValue(Object value);
    }
}
