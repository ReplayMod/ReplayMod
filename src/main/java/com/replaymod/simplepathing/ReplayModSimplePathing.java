package com.replaymod.simplepathing;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.PathingRegistry;
import com.replaymod.pathing.impl.TimelineImpl;
import com.replaymod.pathing.interpolation.AbstractInterpolator;
import com.replaymod.pathing.interpolation.Interpolator;
import com.replaymod.pathing.interpolation.LinearInterpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.simplepathing.gui.GuiPathing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(modid = ReplayModSimplePathing.MOD_ID, useMetadata = true)
public class ReplayModSimplePathing implements PathingRegistry {
    public static final String MOD_ID = "replaymod-simplepathing";

    @Mod.Instance(ReplayMod.MOD_ID)
    private static ReplayMod core;

    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        core.getSettingsRegistry().register(Setting.class);

        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void postReplayOpen(ReplayOpenEvent.Post event) {
        new GuiPathing(core, this, event.getReplayHandler());
    }

    private Timeline currentTimeline = createTimeline(); { currentTimeline.createPath(); currentTimeline.createPath(); }
    private Keyframe selectedTimeKeyframe, selectedPositionKeyframe;

    public Keyframe getSelectedTimeKeyframe() {
        return selectedTimeKeyframe;
    }

    public Keyframe getSelectedPositionKeyframe() {
        return selectedPositionKeyframe;
    }

    public void setSelectedPositionKeyframe(Keyframe selectedPositionKeyframe) {
        this.selectedPositionKeyframe = selectedPositionKeyframe;
    }

    public void setSelectedTimeKeyframe(Keyframe selectedTimeKeyframe) {
        this.selectedTimeKeyframe = selectedTimeKeyframe;
    }

    public void setCurrentTimeline(Timeline currentTimeline) {
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

        return timeline;
    }

    @Override
    public void serializeInterpolator(JsonWriter writer, Interpolator interpolator) throws IOException {
        if (interpolator instanceof LinearInterpolator) {
            writer.value("linear");
        } else {
            throw new IOException("Unknown interpolator type: " + interpolator);
        }
    }

    @Override
    public Interpolator deserializeInterpolator(JsonReader reader) throws IOException {
        AbstractInterpolator interpolator;
        String type = reader.nextString();
        switch (type) {
            case "linear":
                interpolator = new LinearInterpolator();
                interpolator.registerProperty(TimestampProperty.PROPERTY);
                interpolator.registerProperty(CameraProperties.POSITION);
                interpolator.registerProperty(CameraProperties.ROTATION);
                return interpolator;
            default:
                throw new IOException("Unknown interpolation type: " + type);

        }
    }
}
