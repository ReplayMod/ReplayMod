//#if FABRIC>=1
package com.replaymod.core.gui;

import com.replaymod.core.ReplayMod;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public String getModId() {
        return ReplayMod.MOD_ID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return parent -> new GuiReplaySettings(parent, ReplayMod.instance.getSettingsRegistry()).toMinecraft();
    }
}
//#endif
