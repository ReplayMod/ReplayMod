package com.replaymod.render.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.FFmpegWriter;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.CrashException;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.replaymod.core.versions.MCVer.addDetail;
import static com.replaymod.render.ReplayModRender.LOGGER;

public class GuiExportFailed extends GuiScreen {
    public static GuiExportFailed tryToRecover(FFmpegWriter.FFmpegStartupException e, Consumer<RenderSettings> doRestart) {
        // Always log the error first
        LOGGER.error("Rendering video:", e);

        RenderSettings settings = e.getSettings();
        // Check whether the user has configured some custom ffmpeg arguments
        if (settings.getEncodingPreset().getValue().equals(settings.getExportArguments())) {
            // If they haven't, then this is probably a faulty ffmpeg installation and there's nothing we can do
            CrashReport crashReport = CrashReport.create(e, "Exporting video");
            CrashReportSection details = crashReport.addElement("Export details");
            addDetail(details, "Settings", settings::toString);
            addDetail(details, "FFmpeg log", e::getLog);
            throw new CrashException(crashReport);
        } else {
            // If they have, ask them whether it was intentional
            GuiExportFailed gui = new GuiExportFailed(e, doRestart);
            gui.display();
            return gui;
        }
    }

    private final GuiLabel logLabel = new GuiLabel(this)
            .setI18nText("replaymod.gui.rendering.error.ffmpeglog");
    private final GuiVerticalList logList = new GuiVerticalList(this).setDrawShadow(true);
    private final GuiButton resetButton = new GuiButton().setI18nLabel("gui.yes").setSize(100, 20);
    private final GuiButton abortButton = new GuiButton().setI18nLabel("gui.no").setSize(100, 20);
    private final GuiPanel info = new GuiPanel(this)
            .setLayout(new VerticalLayout().setSpacing(4))
            .addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setI18nText("replaymod.gui.rendering.error.ffmpegargs.1"),
                    new GuiLabel().setI18nText("replaymod.gui.rendering.error.ffmpegargs.2"),
                    new GuiLabel(),
                    new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5))
                            .addElements(null, resetButton, abortButton)
            );

    {
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(info, width/2 - width(info)/2, (height/2 - height(info) - 30) / 2 + 30);
                pos(logLabel, width/2 - width(logLabel)/2, height/2 + 4);
                pos(logList, 10, y(logLabel) + height(logLabel) + 4);
                size(logList, width - 10 - x(logList), height - 10 - y(logList));
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.rendering.error.title"));
        setBackground(Background.DIRT);
    }

    public GuiExportFailed(FFmpegWriter.FFmpegStartupException e, Consumer<RenderSettings> doRestart) {
        logList.getListPanel().addElements(null,
                Arrays.stream(e.getLog().replace("\t", "    ").split("\n"))
                        .map(l -> new GuiLabel().setText(l))
                        .toArray(GuiElement[]::new));

        resetButton.onClick(() -> ReplayMod.instance.runLater(() -> {
            RenderSettings oldSettings = e.getSettings();
            doRestart.accept(new RenderSettings(
                    oldSettings.getRenderMethod(),
                    oldSettings.getEncodingPreset(),
                    oldSettings.getVideoWidth(),
                    oldSettings.getVideoHeight(),
                    oldSettings.getFramesPerSecond(),
                    oldSettings.getBitRate(),
                    oldSettings.getOutputFile(),
                    oldSettings.isRenderNameTags(),
                    oldSettings.isStabilizeYaw(),
                    oldSettings.isStabilizePitch(),
                    oldSettings.isStabilizeRoll(),
                    oldSettings.getChromaKeyingColor(),
                    oldSettings.getSphericalFovX(),
                    oldSettings.getSphericalFovY(),
                    oldSettings.isInjectSphericalMetadata(),
                    oldSettings.isDepthMap(),
                    oldSettings.isCameraPathExport(),
                    oldSettings.getAntiAliasing(),
                    oldSettings.getExportCommand(),
                    oldSettings.getEncodingPreset().getValue(),
                    oldSettings.isHighPerformance()
            ));
        }));

        abortButton.onClick(() -> {
            // Assume they know what they're doing
            getMinecraft().openScreen(null);
        });
    }
}
