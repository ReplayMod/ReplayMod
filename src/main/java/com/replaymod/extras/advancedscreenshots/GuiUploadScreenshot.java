package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.extras.Setting;
import com.replaymod.render.RenderSettings;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import org.lwjgl.util.ReadableColor;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class GuiUploadScreenshot extends AbstractGuiPopup<GuiUploadScreenshot> {

    public final ReplayMod mod;

    public final RenderSettings renderSettings;

    public final GuiLabel successLabel = new GuiLabel()
            .setI18nText("replaymod.gui.advancedscreenshots.finished.description")
            .setColor(ReadableColor.BLACK);

    public final GuiLabel veerLabel = new GuiLabel()
            .setI18nText("replaymod.gui.advancedscreenshots.finished.description.veer")
            .setColor(ReadableColor.BLACK);

    public final GuiButton veerUploadButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("replaymod.gui.advancedscreenshots.finished.upload.veer");

    public final GuiButton showOnDiskButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("replaymod.gui.advancedscreenshots.finished.showfile");

    public final GuiButton closeButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("replaymod.gui.close");

    public final GuiCheckbox neverOpenCheckbox = new GuiCheckbox();

    public final GuiLabel neverOpenLabel = new GuiLabel()
            .setI18nText("replaymod.gui.notagain")
            .setColor(ReadableColor.BLACK);

    public final GuiPanel checkboxPanel = GuiPanel.builder()
            .layout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5))
            .with(neverOpenCheckbox, new HorizontalLayout.Data(0.5))
            .with(neverOpenLabel, new HorizontalLayout.Data(0.5))
            .build();

    public GuiUploadScreenshot(GuiContainer container, ReplayMod mod, RenderSettings renderSettings) {
        super(container);
        this.mod = mod;
        this.renderSettings = renderSettings;

        boolean veer = renderSettings.getRenderMethod() == RenderSettings.RenderMethod.EQUIRECTANGULAR;

        if (renderSettings.getRenderMethod() == RenderSettings.RenderMethod.EQUIRECTANGULAR) {
            successLabel.setI18nText("replaymod.gui.advancedscreenshots.finished.description.360");
        }

        if (veer) {
            veerUploadButton.onClick(() -> {
                try {
                    Desktop.getDesktop().browse(URI.create("https://veer.tv/upload"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        showOnDiskButton.onClick(() -> {
            try {
                Desktop.getDesktop().browse(renderSettings.getOutputFile().toURI());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        closeButton.onClick(() -> {
            if (neverOpenCheckbox.isChecked()) {
                SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_SCREENSHOT_GUI, true);
                settingsRegistry.save();
            }
            close();
        });

        popup.addElements(new VerticalLayout.Data(0.5), successLabel);

        if (veer) {
            popup.addElements(new VerticalLayout.Data(0.5),
                    veerLabel,
                    veerUploadButton);
        }

        popup.addElements(new VerticalLayout.Data(0.5),
                successLabel,
                showOnDiskButton,
                closeButton);

        popup.addElements(new VerticalLayout.Data(1),
                checkboxPanel);

        popup.setLayout(new VerticalLayout().setSpacing(5));
    }

    @Override
    protected void open() {
        if (mod.getSettingsRegistry().get(Setting.SKIP_POST_SCREENSHOT_GUI)) {
            return;
        }
        super.open();
    }

    @Override
    protected GuiUploadScreenshot getThis() {
        return this;
    }
}
