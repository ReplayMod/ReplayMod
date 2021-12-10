package com.replaymod.extras;

import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.extras.playeroverview.PlayerOverview;
import com.replaymod.extras.youtube.YoutubeUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReplayModExtras implements Module {
    { instance = this; }
    public static ReplayModExtras instance;

    private static final List<Class<? extends Extra>> builtin = Arrays.asList(
            AdvancedScreenshots.class,
            PlayerOverview.class,
            YoutubeUpload.class,
            FullBrightness.class,
            QuickMode.class
    );

    private final Map<Class<? extends Extra>, Extra> instances = new HashMap<>();

    public static Logger LOGGER = LogManager.getLogger();

    public ReplayModExtras(ReplayMod core) {
        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void initClient() {
        for (Class<? extends Extra> cls : builtin) {
            try {
                Extra extra = cls.newInstance();
                extra.register(ReplayMod.instance);
                instances.put(cls, extra);
            } catch (Throwable t) {
                LOGGER.warn("Failed to load extra " + cls.getName() + ": ", t);
            }
        }
    }

    public <T extends Extra> Optional<T> get(Class<T> cls) {
        return Optional.ofNullable(instances.get(cls)).map(cls::cast);
    }
}
