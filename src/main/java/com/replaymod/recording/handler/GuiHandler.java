package com.replaymod.recording.handler;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.recording.Setting;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiHandler {

    private final ReplayMod mod;

    public GuiHandler(ReplayMod mod) {
        this.mod = mod;
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiWorldSelection || event.getGui() instanceof GuiMultiplayer) {
            SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
            boolean sp = event.getGui() instanceof GuiWorldSelection;
            Setting<Boolean> setting = sp ? Setting.RECORD_SINGLEPLAYER : Setting.RECORD_SERVER;

            GuiCheckbox recordingCheckbox = new GuiCheckbox()
                    .setI18nLabel("replaymod.gui.settings.record" + (sp ? "singleplayer" : "server"))
                    .setChecked(settingsRegistry.get(setting));
            recordingCheckbox.onClick(() -> {
                settingsRegistry.set(setting, recordingCheckbox.isChecked());
                settingsRegistry.save();
            });

            VanillaGuiScreen.setup(event.getGui()).setLayout(new CustomLayout<GuiScreen>() {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    //size(recordingCheckbox, 200, 20);
                    pos(recordingCheckbox, width - width(recordingCheckbox) - 5, 5);
                }
            }).addElements(null, recordingCheckbox);
        }
    }
}
