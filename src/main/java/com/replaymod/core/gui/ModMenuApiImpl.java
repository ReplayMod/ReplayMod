//#if FABRIC>=1
package com.replaymod.core.gui;

import com.replaymod.core.ReplayMod;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

//#if MC>=11700
//$$ import com.terraformersmc.modmenu.api.ConfigScreenFactory;
//$$ import com.terraformersmc.modmenu.api.ModMenuApi;
//#else
import io.github.prospector.modmenu.api.ModMenuApi;
//#endif

public class ModMenuApiImpl implements ModMenuApi {
    //#if MC<11700
    @Override
    public String getModId() {
        return ReplayMod.MOD_ID;
    }
    //#endif

    @Override
    //#if MC>=11700
    //$$ public ConfigScreenFactory<?> getModConfigScreenFactory() {
    //#else
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
    //#endif
        return parent -> new GuiReplaySettings(parent, ReplayMod.instance.getSettingsRegistry()).toMinecraft();
    }
}
//#endif
