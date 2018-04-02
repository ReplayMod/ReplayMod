package com.replaymod.simplepathing;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.replaymod.simplepathing.preview.PathPreview;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

//#if MC>=10800
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import static com.replaymod.core.versions.MCVer.*;

@Mod(modid = ReplayModSimplePathing.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        //#endif
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

        FML_BUS.register(this);

        PathPreview pathPreview = new PathPreview(this);
        pathPreview.register();

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.keyframerepository", Keyboard.KEY_X, () -> {
            if (guiPathing != null) guiPathing.keyframeRepoButtonPressed();
        });
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.clearkeyframes", Keyboard.KEY_C, () -> {
            if (guiPathing != null) guiPathing.clearKeyframesButtonPressed();
        });
        core.getKeyBindingRegistry().registerRepeatedKeyBinding("replaymod.input.synctimeline", Keyboard.KEY_V, () -> {
            if (guiPathing != null) guiPathing.syncTimeButtonPressed();
        });
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_DELETE, () -> {
            if (guiPathing != null) guiPathing.deleteButtonPressed();
        });
    }

    @SubscribeEvent
    public void postReplayOpen(ReplayOpenEvent.Post event) {
        clearCurrentTimeline();
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

    private GuiReplayOverlay getReplayOverlay() {
        return ReplayModReplay.instance.getReplayHandler().getOverlay();
    }

    private SPTimeline currentTimeline;

    private SPPath selectedPath;
    @Getter
    private long selectedTime;

    public SPPath getSelectedPath() {
        if (getReplayOverlay().timeline.getSelectedMarker() != null) {
            selectedPath = null;
            selectedTime = 0;
        }
        return selectedPath;
    }

    public boolean isSelected(Keyframe keyframe) {
        return getSelectedPath() != null && currentTimeline.getKeyframe(selectedPath, selectedTime) == keyframe;
    }

    public void setSelected(SPPath path, long time) {
        selectedPath = path;
        selectedTime = time;
        if (selectedPath != null) {
            getReplayOverlay().timeline.setSelectedMarker(null);
        }
    }

    public void setCurrentTimeline(SPTimeline newTimeline) {
        selectedPath = null;
        currentTimeline = newTimeline;
        updateDefaultInterpolatorType();
    }

    public void clearCurrentTimeline() {
        setCurrentTimeline(new SPTimeline());
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
