package com.replaymod.simplepathing;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.impl.TimelineImpl;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.replaymod.simplepathing.preview.PathPreview;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(modid = ReplayModSimplePathing.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        useMetadata = true)
public class ReplayModSimplePathing implements PathingRegistry {
    public static final String MOD_ID = "replaymod-simplepathing";

    private ReplayMod core;

    public static Logger LOGGER;

    private GuiPathing guiPathing;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        core = ReplayMod.instance;

        core.getSettingsRegistry().register(Setting.class);

        MinecraftForge.EVENT_BUS.register(this);

        PathPreview pathPreview = new PathPreview(this);
        pathPreview.register();
    }

    @SubscribeEvent
    public void postReplayOpen(ReplayOpenEvent.Post event) {
        guiPathing = new GuiPathing(core, this, event.getReplayHandler());
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Post event) {
        guiPathing = null;
        currentTimeline = createTimeline();
        currentTimeline.createPath();
        currentTimeline.createPath();
        selectedKeyframe = null;
    }

    @SubscribeEvent
    public void onSettingsChanged(SettingsChangedEvent event) {
        if (event.getKey() == Setting.LINEAR_INTERPOLATION) {
            if (currentTimeline != null && guiPathing != null) {
                currentTimeline.applyChange(guiPathing.updateInterpolators());
            }
        }
    }

    private Timeline currentTimeline = createTimeline(); { currentTimeline.createPath(); currentTimeline.createPath(); }
    private Keyframe selectedKeyframe;

    public Keyframe getSelectedKeyframe() {
        return selectedKeyframe;
    }

    public void setSelectedKeyframe(Keyframe selected) {
        this.selectedKeyframe = selected;
    }

    public void setCurrentTimeline(Timeline currentTimeline) {
        if (this.currentTimeline != currentTimeline) {
            selectedKeyframe = null;
        }
        this.currentTimeline = currentTimeline;
    }

    public Timeline getCurrentTimeline() {
        return currentTimeline;
    }

    @Override
    public Timeline createTimeline() {
        Timeline timeline = new TimelineImpl();

        timeline.registerProperty(TimestampProperty.PROPERTY);
        timeline.registerProperty(CameraProperties.POSITION);
        timeline.registerProperty(CameraProperties.ROTATION);
        timeline.registerProperty(SpectatorProperty.PROPERTY);

        return timeline;
    }

    @Override
    public void serializeInterpolator(JsonWriter writer, Interpolator interpolator) throws IOException {
        if (interpolator instanceof LinearInterpolator) {
            writer.value("linear");
        } else if (interpolator instanceof CubicSplineInterpolator) {
            writer.value("cubic-spline");
        } else {
            throw new IOException("Unknown interpolator type: " + interpolator);
        }
    }

    @Override
    public Interpolator deserializeInterpolator(JsonReader reader) throws IOException {
        String type = reader.nextString();
        switch (type) {
            case "linear":
                return new LinearInterpolator();
            case "cubic-spline":
                return new CubicSplineInterpolator();
            default:
                throw new IOException("Unknown interpolation type: " + type);

        }
    }

    public ReplayMod getCore() {
        return core;
    }

    public GuiPathing getGuiPathing() {
        return guiPathing;
    }
}
