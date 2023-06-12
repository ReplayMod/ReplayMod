package com.replaymod.recording.gui;

import com.replaymod.core.SettingsRegistry;
import com.replaymod.recording.Setting;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.MinecraftGuiRenderer;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.I18n;

import static com.replaymod.core.ReplayMod.TEXTURE;
import static com.replaymod.core.ReplayMod.TEXTURE_SIZE;
import static com.mojang.blaze3d.platform.GlStateManager.*;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#else
import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * Renders overlay during recording.
 */
public class GuiRecordingOverlay extends EventRegistrations {
    private final MinecraftClient mc;
    private final SettingsRegistry settingsRegistry;
    private final GuiRecordingControls guiControls;

    public GuiRecordingOverlay(MinecraftClient mc, SettingsRegistry settingsRegistry, GuiRecordingControls guiControls) {
        this.mc = mc;
        this.settingsRegistry = settingsRegistry;
        this.guiControls = guiControls;
    }

    /**
     * Render the recording icon and text in the top left corner of the screen.
     */
    { on(RenderHudCallback.EVENT, (stack, partialTicks) -> renderRecordingIndicator(stack)); }
    //#if MC>=12000
    //$$ private void renderRecordingIndicator(DrawContext stack) {
    //#else
    private void renderRecordingIndicator(MatrixStack stack) {
    //#endif
        if (guiControls.isStopped()) return;
        if (settingsRegistry.get(Setting.INDICATOR)) {
            TextRenderer fontRenderer = mc.textRenderer;
            String text = guiControls.isPaused() ? I18n.translate("replaymod.gui.paused") : I18n.translate("replaymod.gui.recording");
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            renderer.drawString(30, 18 - (fontRenderer.fontHeight / 2), 0xffffffff, text.toUpperCase());
            renderer.bindTexture(TEXTURE);
            //#if MC<11700
            enableAlphaTest();
            //#endif
            renderer.drawTexturedRect(10, 10, 58, 20, 16, 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
