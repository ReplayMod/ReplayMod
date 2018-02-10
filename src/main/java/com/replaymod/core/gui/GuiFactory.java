package com.replaymod.core.gui;

import com.replaymod.core.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

@SuppressWarnings("unused")
public class GuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {

    }

    //#if MC>=11200
    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiReplaySettings(parentScreen, ReplayMod.instance.getSettingsRegistry()).toMinecraft();
    }
    //#else
    //$$ @Override
    //$$ public Class<? extends GuiScreen> mainConfigGuiClass() {
    //$$     return ConfigGuiWrapper.class;
    //$$ }
    //$$
    //$$ @Override
    //$$ public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
    //$$     return null;
    //$$ }
    //$$
    //$$ public static class ConfigGuiWrapper extends GuiScreen {
    //$$     private final GuiScreen parent;
    //$$
    //$$     public ConfigGuiWrapper(GuiScreen parent) {
    //$$         this.parent = parent;
    //$$     }
    //$$
    //$$     @Override
    //$$     public void initGui() {
    //$$         new GuiReplaySettings(parent, ReplayMod.instance.getSettingsRegistry()).display();
    //$$     }
    //$$ }
    //#endif

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}
