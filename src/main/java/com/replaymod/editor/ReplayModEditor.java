package com.replaymod.editor;

import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.editor.handler.GuiHandler;
import com.replaymod.online.Setting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReplayModEditor implements Module {
    { instance = this; }
    public static ReplayModEditor instance;

    private ReplayMod core;

    public static Logger LOGGER = LogManager.getLogger();

    public ReplayModEditor(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void initClient() {
        new GuiHandler().register();
    }

    public ReplayMod getCore() {
        return core;
    }
}
