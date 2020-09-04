package com.replaymod.editor.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.replay.gui.overlay.GuiMarkerTimeline;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiHorizontalScrollbar;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.element.advanced.GuiTimelineTime;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.util.crash.CrashReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuiEditReplay extends AbstractGuiPopup<GuiEditReplay> {
    private final Path inputPath;

    private final EditTimeline timeline;

    private final GuiTimelineTime<GuiMarkerTimeline> timelineTime = new GuiTimelineTime<>();

    private final GuiHorizontalScrollbar scrollbar = new GuiHorizontalScrollbar().setSize(300, 9);


    private final GuiButton zoomInButton = new GuiButton().setSize(9, 9)
            .onClick(() -> zoomTimeline(2d / 3d))
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setSpriteUV(40, 20)
            .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.ingame.menu.zoomin"));

    private final GuiButton zoomOutButton = new GuiButton().setSize(9, 9)
            .onClick(() -> zoomTimeline(3d / 2d))
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setSpriteUV(40, 30)
            .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.ingame.menu.zoomout"));

    private final GuiPanel zoomButtonPanel = new GuiPanel()
            .setLayout(new VerticalLayout(VerticalLayout.Alignment.CENTER).setSpacing(2))
            .addElements(null, zoomInButton, zoomOutButton);

    private Set<Marker> markers;

    protected GuiEditReplay(GuiContainer container, Path inputPath) throws IOException {
        super(container);
        this.inputPath = inputPath;

        try (ZipReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), inputPath.toFile())) {
            markers = replayFile.getMarkers().or(HashSet::new);
            timeline = new EditTimeline(new HashSet<>(markers), markers -> this.markers = markers);
            timeline.setSize(300, 20)
                    .setMarkers()
                    .setLength(replayFile.getMetaData().getDuration())
                    .onClick(timeline::setCursorPosition);
        }

        timelineTime.setTimeline(timeline);

        scrollbar.onValueChanged(() -> {
            timeline.setOffset((int) (scrollbar.getPosition() * timeline.getLength()));
            timeline.setZoom(scrollbar.getZoom());
        }).setZoom(1);

        GuiPanel timelinePanel = new GuiPanel()
                .setSize(300, 40)
                .setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        pos(zoomButtonPanel, width - width(zoomButtonPanel), 10);
                        pos(timelineTime, 0, 2);
                        size(timelineTime, x(zoomButtonPanel), 8);
                        pos(timeline, 0, y(timelineTime) + height(timelineTime));
                        size(timeline, x(zoomButtonPanel) - 2, 20);
                        pos(scrollbar, 0, y(timeline) + height(timeline) + 1);
                        size(scrollbar, x(zoomButtonPanel) - 2, 9);
                    }
                })
                .addElements(null, timelineTime, timeline, scrollbar, zoomButtonPanel);

        GuiButton buttonAddSplit = new GuiButton()
                .setSize(100, 20)
                .setI18nLabel("replaymod.gui.edit.split")
                .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.edit.split.tooltip"))
                .onClick(() -> {
                    Marker marker = new Marker();
                    marker.setTime(timeline.getCursorPosition());
                    marker.setName(MarkerProcessor.MARKER_NAME_SPLIT);
                    timeline.addMarker(marker);
                });

        GuiButton buttonInsertCut = new GuiButton()
                .setSize(100, 20)
                .setI18nLabel("replaymod.gui.edit.cut.start")
                .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.edit.cut.tooltip"))
                .onClick(() -> {
                    Marker marker = new Marker();
                    marker.setTime(timeline.getCursorPosition());
                    marker.setName(MarkerProcessor.MARKER_NAME_START_CUT);
                    timeline.addMarker(marker);
                });

        GuiButton buttonEndCut = new GuiButton()
                .setSize(100, 20)
                .setI18nLabel("replaymod.gui.edit.cut.end")
                .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.edit.cut.tooltip"))
                .onClick(() -> {
                    Marker marker = new Marker();
                    marker.setTime(timeline.getCursorPosition());
                    marker.setName(MarkerProcessor.MARKER_NAME_END_CUT);
                    timeline.addMarker(marker);
                });

        GuiPanel controlPanel = new GuiPanel()
                .setLayout(new HorizontalLayout().setSpacing(4))
                .addElements(null, buttonAddSplit, buttonInsertCut, buttonEndCut);

        GuiButton applyButton = new GuiButton().setI18nLabel("replaymod.gui.edit.apply").setSize(150, 20).onClick(this::apply);
        GuiButton closeButton = new GuiButton().setI18nLabel("replaymod.gui.close").setSize(150, 20).onClick(this::close);
        GuiPanel buttonPanel = new GuiPanel()
                .setLayout(new HorizontalLayout().setSpacing(8))
                .addElements(null, applyButton, closeButton);

        popup.setLayout(new VerticalLayout(VerticalLayout.Alignment.TOP).setSpacing(10));
        popup.addElements(new VerticalLayout.Data(0.5), timelinePanel, controlPanel, buttonPanel);
    }

    private void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    private void apply() {
        ProgressPopup progressPopup = new ProgressPopup(this);

        new Thread(() -> {
            try (ZipReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), inputPath.toFile())) {
                replayFile.writeMarkers(markers);
                replayFile.save();
            } catch (IOException e) {
                Utils.error(ReplayModEditor.LOGGER, this, CrashReport.create(e, "Writing markers"), this::close);
            }

            try {
                MarkerProcessor.apply(inputPath, progressPopup.progressBar::setProgress);

                ReplayMod.instance.runLater(() -> {
                    progressPopup.close();
                    close();
                });
            } catch (Throwable e) {
                e.printStackTrace(); // in case runLater fails
                CrashReport crashReport = CrashReport.create(e, "Running marker processor");
                ReplayMod.instance.runLater(() -> Utils.error(ReplayModEditor.LOGGER, this, crashReport, () -> {
                    progressPopup.close();
                    close();
                }));
            }
        }).start();
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    protected GuiEditReplay getThis() {
        return this;
    }

    private class ProgressPopup extends AbstractGuiPopup<ProgressPopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar(popup).setSize(300, 20);

        ProgressPopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected ProgressPopup getThis() {
            return this;
        }
    }

    private class EditTimeline extends GuiMarkerTimeline {
        EditTimeline(Set<Marker> markers, Consumer<Set<Marker>> saveMarkers) {
            super(markers, saveMarkers);
        }

        @Override
        protected void drawMarkers(GuiRenderer renderer, ReadableDimension size) {
            drawCutQuads(renderer, size);
            super.drawMarkers(renderer, size);
        }

        @Override
        protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker, int markerX) {
            if (MarkerProcessor.MARKER_NAME_SPLIT.equals(marker.getName())) {
                int height = size.getHeight() - BORDER_BOTTOM - BORDER_TOP - MARKER_SIZE + 1;
                for (int y = 0; y < height; y += 3) {
                    renderer.drawRect(markerX, BORDER_TOP + y, 1, 2, Color.WHITE);
                }
            }

            super.drawMarker(renderer, size, marker, markerX);
        }

        private void drawCutQuads(GuiRenderer renderer, ReadableDimension size) {
            boolean inCut = false;
            int startTime = 0;
            for (Marker marker : markers.stream().sorted(Comparator.comparing(Marker::getTime)).collect(Collectors.toList())) {
                if (MarkerProcessor.MARKER_NAME_START_CUT.equals(marker.getName()) && !inCut) {
                    inCut = true;
                    startTime = marker.getTime();
                } else if (MarkerProcessor.MARKER_NAME_END_CUT.equals(marker.getName()) && inCut) {
                    drawCutQuad(renderer, size, startTime, marker.getTime());
                    inCut = false;
                }
            }

            if (inCut) {
                drawCutQuad(renderer, size, startTime, getLength());
            }
        }

        private void drawCutQuad(GuiRenderer renderer, ReadableDimension size, int startFrameTime, int endFrameTime) {
            int visibleWidth = size.getWidth() - BORDER_LEFT - BORDER_RIGHT;
            int startTime = getOffset();
            int visibleTime = (int) (getZoom() * getLength());
            int endTime = getOffset() + visibleTime;

            if (startFrameTime >= endTime || endFrameTime <= startTime) {
                return; // Segment out of display range
            }

            double relativeStart = startFrameTime - startTime;
            double relativeEnd = endFrameTime - startTime;
            int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + MARKER_SIZE / 2 + 1);
            int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - MARKER_SIZE / 2);
            if (startX < endX) {
                renderer.drawRect(startX + 1, size.getHeight() - BORDER_BOTTOM - MARKER_SIZE,
                        endX - startX - 2, MARKER_SIZE - 2, Color.RED);
            }
        }
    }
}
