package com.replaymod.editor.gui;

import com.google.common.io.Files;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import net.minecraft.crash.CrashReport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.ReadableDimension;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.replaymod.editor.ReplayModEditor.LOGGER;

public class GuiReplayEditor extends GuiScreen {
    private final ReplayMod mod;

    private volatile ProcessingStage processingStage;
    private volatile double progress;
    private GuiProcessingProgressPopup popup;

    public GuiButton currentTabButton;
    public GuiPanel currentTabPanel;

    public final GuiLabel warningLabel = new GuiLabel(this).setColor(Colors.RED)
            .setI18nText("replaymod.gui.editor.disclaimer");

    public final GuiPanel tabButtons = new GuiPanel(this).setLayout(new GridLayout().setSpacingX(5));
    public final List<GuiPanel> tabPanels = new ArrayList<>();

    public final GuiButton saveButton = new GuiButton().setI18nLabel("replaymod.gui.save").setSize(100, 20);
    public final GuiButton backButton = new GuiButton().setI18nLabel("replaymod.gui.back").setSize(100, 20);

    public final GuiPanel buttonPanel = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, saveButton, backButton);

    public GuiReplayEditor(GuiScreen parent, ReplayMod mod) {
        this.mod = mod;
        backButton.onClick(parent::display);

        setTitle(new GuiLabel().setI18nText("replaymod.gui.replayeditor"));

        makeTab("trim", () -> new GuiTrimPanel(this));
        makeTab("connect", GuiPanel::new).setDisabled(); // Not yet implemented
        makeTab("modify", GuiPanel::new).setDisabled(); // Not yet implemented

        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                // Move all inactive panels aside
                tabPanels.forEach(e -> pos(e, Integer.MIN_VALUE, Integer.MIN_VALUE));

                pos(warningLabel, 10, 22);
                size(warningLabel, width - 20, 10);

                pos(tabButtons, 10, y(warningLabel) + height(warningLabel) + 2);
                size(tabButtons, width - 20, 20);

                pos(buttonPanel, width - 10 - width(buttonPanel), height - 10 - height(buttonPanel));

                if (currentTabPanel != null) {
                    pos(currentTabPanel, 10, y(tabButtons) + height(tabButtons) + 10);
                    size(currentTabPanel, width - 20, y(buttonPanel) - 10 - y(currentTabPanel));
                }
            }
        });
    }

    public GuiButton makeTab(String name, Supplier<GuiPanel> panel) {
        GuiButton button =  new GuiButton().setI18nLabel("replaymod.gui.editor." + name + ".title")
                .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.editor." + name + ".description"))
                .setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        button.onClick(() -> {
            // Remove previous tab
            if (currentTabButton != null) {
                currentTabButton.setEnabled();
                removeElement(currentTabPanel);
            }

            // Activate new tab
            currentTabButton = button.setDisabled();
            addElements(null, currentTabPanel = panel.get());
        });
        // Add new tab button to tabs panel
        tabButtons.addElements(null, button);
        ((GridLayout) tabButtons.getLayout()).setColumns(tabButtons.getChildren().size());
        return button;
    }

    @SafeVarargs
    public final void save(File inputFile, Pair<PacketStream.FilterInfo, JsonObject>... filters) {
        save(Utils.fileNameToReplayName(inputFile.getName()), (outputFile) -> {
            Studio studio = new ReplayStudio();
            File tmpDir = null;
            try {
                File actualOutputFile  = outputFile;
                if (outputFile.getCanonicalPath().equals(inputFile.getCanonicalPath())) {
                    // Input and output files are identical. Due to the way the ZipReplayFile stores its temporary
                    // data, the same replay file must not be opened twice for writing (tmp files will be deleted once
                    // either one is closed or, on Windows, will throw an exception when deleted).
                    tmpDir = Files.createTempDir();
                    outputFile = new File(tmpDir, "replay.mcpr");
                    LOGGER.debug("Output file is identical to input file, using temporary output file {} instead",
                            outputFile);
                }
                try (ReplayFile outputReplay = new ZipReplayFile(studio, inputFile, outputFile);
                     ReplayOutputStream out = outputReplay.writePacketData();
                     ReplayFile inputReplay = new ZipReplayFile(studio, inputFile);
                     ReplayInputStream in = inputReplay.getPacketData()) {
                    ReplayMetaData metaData = inputReplay.getMetaData();
                    PacketStream stream = studio.createReplayStream(in, true);

                    stream.addFilter(new ProgressFilter(metaData.getDuration()));
                    for (Pair<PacketStream.FilterInfo, JsonObject> pair : filters) {
                        PacketStream.FilterInfo info = pair.getLeft();
                        JsonObject config = pair.getRight();
                        info.getFilter().init(studio, config);
                        stream.addFilter(info.getFilter(), info.getFrom(), info.getTo());
                        LOGGER.debug("Added filter {}", info);
                    }

                    LOGGER.info("Built pipeline: {}", stream);
                    stream.start();

                    long lastTimestamp = 0;

                    PacketData data;
                    while ((data = stream.next()) != null) {
                        out.write(data);
                        lastTimestamp = data.getTime();
                    }

                    for (PacketData d : stream.end()) {
                        out.write(d);
                        lastTimestamp = d.getTime();
                    }

                    updateProgress(ProcessingStage.FINALIZING, 0);

                    // Update duration of new replay
                    metaData.setDuration((int) lastTimestamp);
                    outputReplay.writeMetaData(metaData);

                    out.close();
                    outputReplay.save();
                }
                if (outputFile != actualOutputFile) {
                    LOGGER.debug("Moving temporary output file {} to {}");
                    FileUtils.forceDelete(actualOutputFile);
                    FileUtils.moveFile(outputFile, actualOutputFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (tmpDir != null && !FileUtils.deleteQuietly(tmpDir)) {
                    LOGGER.warn("Failed to delete temporary directory {}", tmpDir);
                }
            }
        });
    }

    public void save(String sourceName, Consumer<File> runnable) {
        GuiTextField nameField = new GuiTextField().setSize(200, 20).setFocused(true).setText(sourceName);
        GuiYesNoPopup popup = GuiYesNoPopup.open(this,
                nameField
        ).setYesI18nLabel("replaymod.gui.editor.savemode.override").setNoI18nLabel("replaymod.gui.cancel");
        nameField.onEnter(() -> {
            if (popup.getYesButton().isEnabled()) {
                popup.getYesButton().onClick();
            }
        }).onTextChanged(obj -> {
            popup.getYesButton().setEnabled(!nameField.getText().isEmpty());
            popup.setYesI18nLabel("replaymod.gui.editor.savemode." +
                    (nameField.getText().equals(sourceName) ? "override" : "newfile"));
        });
        Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                if (result == Boolean.TRUE) {
                    // Sanitize their input
                    String name = nameField.getText().trim().replace("[^a-zA-Z0-9\\.\\- ]", "_");
                    // This file is what they want
                    File replayFolder;
                    try {
                        replayFolder = mod.getReplayFolder();
                    } catch (IOException e) {
                        Utils.error(LOGGER, GuiReplayEditor.this, CrashReport.makeCrashReport(e, "Getting replay folder"), null);
                        return;
                    }
                    File targetFile = new File(replayFolder, Utils.replayNameToFileName(name));
                    if (targetFile.exists()) {
                        LOGGER.trace("Selected file already exists, asking for confirmation");
                        Futures.addCallback(GuiYesNoPopup.open(GuiReplayEditor.this,
                                new GuiLabel().setColor(Colors.BLACK)
                                        .setI18nText("replaymod.gui.replaymodified.warning1", name),
                                new GuiLabel().setColor(Colors.BLACK)
                                        .setI18nText("replaymod.gui.replaymodified.warning2"))
                                .setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no")
                                .getFuture(), new FutureCallback<Boolean>() {
                            @Override
                            public void onSuccess(@Nullable Boolean result) {
                                if (result == Boolean.TRUE) {
                                    LOGGER.trace("Saving output in {}", targetFile);
                                    runEdit(() -> runnable.accept(targetFile));
                                } else {
                                    LOGGER.trace("Not overwriting file {}, reopening naming dialog", targetFile);
                                    save(sourceName, runnable);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOGGER.error("During save file confirmation dialog:", t);
                            }
                        });
                    } else {
                        LOGGER.trace("Saving output in {}", targetFile);
                        runEdit(() -> runnable.accept(targetFile));
                    }
                } else {
                    LOGGER.trace("Saving cancelled");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("During save file dialog:", t);
            }
        });
    }

    public void runEdit(Runnable runnable) {
        popup = new GuiProcessingProgressPopup(this);
        processingStage = ProcessingStage.INITIALIZING;
        progress = 0;
        new Thread(() -> {
            LOGGER.info("Starting editing of replay");
            try {
                runnable.run();
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Editing replay file");
                mod.runLater(() -> Utils.error(LOGGER, this, crashReport, popup::close));
                return;
            }
            LOGGER.info("Successfully finished editing");
            mod.runLater(() -> {
                popup.close();
                GuiInfoPopup.open(this, "replaymod.gui.editor.progress.status.finished");
            });
        }).start();
    }

    public void updateProgress(ProcessingStage stage, double progress) {
        if (this.processingStage != stage) {
            LOGGER.trace("Changing state from {} to {}", this.processingStage, stage);
        }
        this.processingStage = stage;
        this.progress = progress;
    }

    public ReplayMod getMod() {
        return mod;
    }

    public enum ProcessingStage {
        INITIALIZING("initializing", 0.01),
        WORKING("writing.raw", 0.89),
        FINALIZING("writing.final", 0.1),
        DONE("finished", 0);

        private final String i18nKey;
        private final double progressFraction;

        ProcessingStage(String i18nKey, double progressFraction) {
            this.i18nKey = "replaymod.gui.editor.progress.status." + i18nKey;
            this.progressFraction = progressFraction;
        }

        public double getOverallProgress(double progress) {
            progress *= progressFraction;
            for (ProcessingStage processingStage : values()) {
                if (processingStage == this) break;
                progress += processingStage.progressFraction;
            }
            return progress;
        }
    }

    private class ProgressFilter implements StreamFilter {
        private final long total;

        public ProgressFilter(long total) {
            this.total = total;
        }

        @Override
        public String getName() {
            return "progress";
        }

        @Override
        public void init(Studio studio, JsonObject config) {

        }

        @Override
        public void onStart(PacketStream stream) {
            updateProgress(ProcessingStage.WORKING, 0);
        }

        @Override
        public boolean onPacket(PacketStream stream, PacketData data) {
            updateProgress(ProcessingStage.WORKING, (double) data.getTime() / total);
            return true;
        }

        @Override
        public void onEnd(PacketStream stream, long timestamp) {}
    }

    private class GuiProcessingProgressPopup extends AbstractGuiPopup<GuiProcessingProgressPopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar().setSize(300, 20);

        {
            popup.setLayout(new VerticalLayout().setSpacing(10));
            popup.addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setI18nText("replaymod.gui.editor.progress.title").setColor(Colors.BLACK),
                    new GuiLabel().setI18nText("replaymod.gui.editor.progress.pleasewait").setColor(Colors.BLACK),
                    progressBar);
        }

        public GuiProcessingProgressPopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            // Update progress bar
            progressBar.setI18nLabel(processingStage.i18nKey);
            progressBar.setProgress((float) processingStage.getOverallProgress(progress));

            super.draw(renderer, size, renderInfo);
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected GuiProcessingProgressPopup getThis() {
            return this;
        }
    }
}
