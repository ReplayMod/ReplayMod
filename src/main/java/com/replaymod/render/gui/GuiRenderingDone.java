package com.replaymod.render.gui;

import com.replaymod.core.SettingsRegistry;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.Setting;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiCheckbox;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.Sys;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class GuiRenderingDone extends GuiScreen {
    public final ReplayModRender mod;
    public final File videoFile;
    public final int videoFrames;
    public final RenderSettings settings;

    public final GuiLabel infoLine1 = new GuiLabel().setI18nText("replaymod.gui.renderdone1");
    public final GuiLabel infoLine2 = new GuiLabel().setI18nText("replaymod.gui.renderdone2");

    public final GuiButton openFolder = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            File folder = videoFile.getParentFile();
            String path = folder.getAbsolutePath();

            // First try OS specific methods
            try {
                switch (Util.getOSType()) {
                    case WINDOWS:
                        Runtime.getRuntime().exec(String.format("cmd.exe /C start \"Open file\" \"%s\"", path));
                        return;
                    case OSX:
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", path});
                        return;
                }
            } catch (IOException e) {
                LogManager.getLogger().error("Cannot open file", e);
            }

            // Otherwise try the java way
            try {
                Desktop.getDesktop().browse(folder.toURI());
            } catch (Throwable throwable) {
                // And if all fails, lwjgl
                Sys.openURL("file://" + path);
            }
        }
    }).setSize(200, 20).setI18nLabel("replaymod.gui.openfolder");

    public final GuiPanel actionsPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10))
            .addElements(null, openFolder);

    public final GuiPanel mainPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(10))
            .addElements(new VerticalLayout.Data(0.5),
                    new GuiPanel().setLayout(new VerticalLayout().setSpacing(4)).addElements(null, infoLine1, infoLine2),
                    actionsPanel);

    public final GuiButton closeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            if (neverOpenCheckbox.isChecked()) {
                SettingsRegistry settingsRegistry = mod.getCore().getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_RENDER_GUI, true);
                settingsRegistry.save();
            }
            getMinecraft().displayGuiScreen(null);
        }
    }).setSize(100, 20).setI18nLabel("replaymod.gui.close");

    public final GuiCheckbox neverOpenCheckbox = new GuiCheckbox().setI18nLabel("replaymod.gui.notagain");

    public final GuiPanel closePanel = new GuiPanel(this)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5))
            .addElements(new HorizontalLayout.Data(0.5), neverOpenCheckbox, closeButton);

    {
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(mainPanel, width / 2 - width(mainPanel) / 2, height / 3 - height(mainPanel) / 2);
                pos(closePanel, width - 10 - width(closePanel), height - 10 - height(closePanel));
            }
        });
        setTitle(new GuiLabel().setI18nText("replaymod.gui.renderdonetitle"));
        setBackground(Background.DIRT);
    }

    @Override
    public void display() {
        if (mod.getCore().getSettingsRegistry().get(Setting.SKIP_POST_RENDER_GUI)) {
            return;
        }
        super.display();
    }
}
