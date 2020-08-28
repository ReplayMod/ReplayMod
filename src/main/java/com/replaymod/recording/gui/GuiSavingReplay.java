package com.replaymod.recording.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.function.Focusable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.getMinecraft;
import static de.johni0702.minecraft.gui.utils.Utils.link;

public class GuiSavingReplay {

    private static final MinecraftClient mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final GuiLabel label = new GuiLabel()
            .setI18nText("replaymod.gui.replaysaving.title")
            .setColor(Colors.BLACK);

    private final GuiProgressBar progressBar = new GuiProgressBar()
            .setHeight(14);

    private final GuiPanel panel = new GuiPanel()
            .setLayout(new VerticalLayout().setSpacing(2))
            .addElements(new VerticalLayout.Data(0.5), label, progressBar);

    private final ReplayMod core;
    private final List<Runnable> apply = new ArrayList<>();

    public GuiSavingReplay(ReplayMod core) {
        this.core = core;
    }

    public void open() {
        core.getBackgroundProcesses().addProcess(panel);
    }

    public void close() {
        core.getBackgroundProcesses().removeProcess(panel);
    }

    public GuiProgressBar getProgressBar() {
        return progressBar;
    }

    public void presentRenameDialog(List<Pair<Path, ReplayMetaData>> outputPaths) {
        panel.removeElement(progressBar);

        link(outputPaths.stream().map(it -> addOutput(it.getKey(), it.getValue())).toArray(Focusable[]::new));

        GuiButton applyButton = new GuiButton()
                .setSize(150, 20)
                .setI18nLabel("replaymod.gui.done")
                .onClick(() -> {
                    apply.forEach(Runnable::run);
                    close();
                });

        panel.addElements(new VerticalLayout.Data(0.5), applyButton);
    }

    private GuiTextField addOutput(Path path, ReplayMetaData metaData) {
        String originalName = Utils.fileNameToReplayName(path.getFileName().toString());
        GuiTextField textField = new GuiTextField()
                .setSize(130, 20)
                .setText(originalName)
                .setI18nHint("replaymod.gui.delete")
                .setTextColorDisabled(Colors.RED)
                .setTooltip(createTooltip(path, metaData));
        GuiButton clearButton = new GuiButton()
                .setSize(20, 20)
                .setLabel("X")
                .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.delete"))
                .onClick(() -> textField.setText(""));
        GuiPanel row = new GuiPanel()
                .setLayout(new HorizontalLayout())
                .addElements(null, textField, clearButton);
        panel.addElements(new VerticalLayout.Data(0.5), row);

        apply.add(() -> {
            String newName = textField.getText();
            if (newName.equals(originalName)) {
                return;
            }
            applyOutput(path, newName);
        });

        return textField;
    }

    private GuiPanel createTooltip(Path path, ReplayMetaData metaData) {
        GuiTooltip tooltip = new GuiTooltip();
        GuiReplayViewer.GuiReplayEntry entry = new GuiReplayViewer.GuiReplayEntry(path.toFile(), metaData, null);
        return new GuiPanel().setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(entry, 4, 4);
                size(entry, width - 8, height - 8);
                size(tooltip, width, height);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension size = entry.calcMinSize();
                return new Dimension(size.getWidth() + 8, size.getHeight() + 8);
            }
        }).addElements(null, tooltip, entry);
    }

    private void applyOutput(Path path, String newName) {
        if (newName.isEmpty()) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.error("Deleting replay file:", e);
                CrashReport crashReport = CrashReport.create(e, "Deleting replay file");
                core.runLater(() -> Utils.error(logger, VanillaGuiScreen.setup(mc.currentScreen), crashReport, () -> {}));
            }
            return;
        }

        Path newPath = path.resolveSibling(Utils.replayNameToFileName(newName));
        if (Files.exists(newPath)) {
            return;
        }

        try {
            Files.move(path, newPath);
        } catch (IOException e) {
            logger.error("Renaming replay file:", e);
            CrashReport crashReport = CrashReport.create(e, "Renaming replay file");
            core.runLater(() -> Utils.error(logger, VanillaGuiScreen.setup(mc.currentScreen), crashReport, () -> {}));
        }
    }
}
