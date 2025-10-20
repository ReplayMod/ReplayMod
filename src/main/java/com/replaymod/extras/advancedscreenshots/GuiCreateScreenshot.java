package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.ReplayMod;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.gui.GuiRenderSettings;
import com.replaymod.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.function.Loadable;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import net.minecraft.util.crash.CrashReport;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.replaymod.core.utils.Utils.error;
import static com.replaymod.render.ReplayModRender.LOGGER;

public class GuiCreateScreenshot extends GuiRenderSettings implements Loadable {

    private final ReplayMod mod;

    public GuiCreateScreenshot(ReplayMod mod) {
        super(GuiRenderSettings.createBaseScreen(), null, null);

        this.mod = mod;

        resetChildren(settingsList.getListPanel()).addElements(new VerticalLayout.Data(0.5),
                new GuiLabel().setI18nText("replaymod.gui.advancedscreenshots.title"), mainPanel, new GuiPanel(),
                new GuiLabel().setI18nText("replaymod.gui.rendersettings.advanced"), advancedPanel, new GuiPanel());

        resetChildren(mainPanel).addElements(new GridLayout.Data(1, 0.5),
                new GuiLabel().setI18nText("replaymod.gui.rendersettings.renderer"), renderMethodDropdown,
                new GuiLabel().setI18nText("replaymod.gui.advancedscreenshots.resolution"), videoResolutionPanel,
                new GuiLabel().setI18nText("replaymod.gui.rendersettings.outputfile"), outputFileButton);

        resetChildren(advancedPanel).addElements(null, nametagCheckbox, alphaCheckbox , new GuiPanel().setLayout(
                new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(15))
                .addElements(new GridLayout.Data(0, 0.5),
                        new GuiLabel().setI18nText("replaymod.gui.rendersettings.stabilizecamera"), stabilizePanel,
                        chromaKeyingCheckbox, chromaKeyingColor));

        exportArguments.setText(""); // To disable any preset-based checks
        buttonPanel.removeElement(queueButton);
        renderButton.setI18nLabel("replaymod.gui.advancedscreenshots.create").onClick(click -> {
            // Closing this GUI ensures that settings are saved
            close();

            mod.runLater(() -> {
                try {
                    RenderSettings settings = save(false, click.hasCtrl());

                    boolean success = new ScreenshotRenderer(settings).renderScreenshot();
                    if (success) {
                        GuiScreen screen = createBaseScreen();
                        new GuiUploadScreenshot(screen, mod, settings).open();
                        screen.display();
                    }

                } catch (Throwable t) {
                    error(LOGGER, GuiCreateScreenshot.this, CrashReport.create(t, "Rendering video"), () -> {});
                    getScreen().display(); // Re-show the render settings gui and the new error popup
                }
            });
        });
    }

    private <T extends GuiContainer<?>> T resetChildren(T container) {
        new ArrayList<>(container.getChildren()).forEach(container::removeElement);
        return container;
    }

    @Override
    public void open() {
        super.open();
        getScreen().display();
    }

    @Override
    public void close() {
        super.close();
        getMinecraft().openScreen(null);
    }

    @Override
    public void load() {
        // pause replay when opening this gui
        ReplayModReplay.instance.getReplayHandler().getReplaySender().setReplaySpeed(0);
    }

    @Override
    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        File screenshotFolder = new File(getMinecraft().runDirectory, "screenshots");
        screenshotFolder.mkdirs();
        String baseName = DATE_FORMAT.format(new Date());
        for (int i = 1; ; i++) {
            File screenshotFile = new File(screenshotFolder, baseName + (i == 1 ? "" : "_" + i) + ".png");
            if (!screenshotFile.exists()) {
                return screenshotFile;
            }
        }
    }

    @Override
    public void load(RenderSettings settings) {
        super.load(settings.withEncodingPreset(RenderSettings.EncodingPreset.PNG));
    }

    @Override
    protected Path getSettingsPath() {
        return getMinecraft().runDirectory.toPath().resolve("config/replaymod-screenshotsettings.json");
    }
}