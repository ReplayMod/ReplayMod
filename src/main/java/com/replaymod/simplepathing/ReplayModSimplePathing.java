package com.replaymod.simplepathing;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.replaymod.simplepathing.preview.PathPreview;
import lombok.Getter;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ReplayModSimplePathing.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        useMetadata = true)
public class ReplayModSimplePathing {
    public static final String MOD_ID = "replaymod-simplepathing";

    @Mod.Instance(MOD_ID)
    public static ReplayModSimplePathing instance;

    private ReplayMod core;

    public static Logger LOGGER;

    private GuiPathing guiPathing;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        core = ReplayMod.instance;

        core.getSettingsRegistry().register(Setting.class);

        FMLCommonHandler.instance().bus().register(this);

        PathPreview pathPreview = new PathPreview(this);
        pathPreview.register();
    }

    @SubscribeEvent
    public void postReplayOpen(ReplayOpenEvent.Post event) {
        currentTimeline = new SPTimeline();
        guiPathing = new GuiPathing(core, this, event.getReplayHandler());
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Post event) {
        currentTimeline = null;
        guiPathing = null;
        selectedPath = null;
    }

    @SubscribeEvent
    public void onSettingsChanged(SettingsChangedEvent event) {
        if (event.getKey() == Setting.DEFAULT_INTERPOLATION) {
            if (currentTimeline != null && guiPathing != null) {
                updateDefaultInterpolatorType();
            }
        }
    }

    private SPTimeline currentTimeline;

    @Getter
    private SPPath selectedPath;
    @Getter
    private long selectedTime;

    public boolean isSelected(Keyframe keyframe) {
        return selectedPath != null && currentTimeline.getKeyframe(selectedPath, selectedTime) == keyframe;
    }

    public void setSelected(SPPath path, long time) {
        selectedPath = path;
        selectedTime = time;
    }

    public void setCurrentTimeline(SPTimeline newTimeline) {
        selectedPath = null;
        currentTimeline = newTimeline;
    }

    public void clearCurrentTimeline() {
        setCurrentTimeline(new SPTimeline());
        updateDefaultInterpolatorType();
    }

    public SPTimeline getCurrentTimeline() {
        return currentTimeline;
    }

    private void updateDefaultInterpolatorType() {
        InterpolatorType newDefaultType =
                InterpolatorType.fromString(core.getSettingsRegistry().get(Setting.DEFAULT_INTERPOLATION));
        currentTimeline.setDefaultInterpolatorType(newDefaultType);
    }

    public ReplayMod getCore() {
        return core;
    }

    public GuiPathing getGuiPathing() {
        return guiPathing;
    }
}
