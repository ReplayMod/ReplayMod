package com.replaymod.editor.gui;

import com.google.gson.JsonObject;
import com.replaymod.core.utils.Utils;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.filter.ChangeTimestampFilter;
import com.replaymod.replaystudio.filter.RemoveFilter;
import com.replaymod.replaystudio.filter.SquashFilter;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiNumberField;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.lwjgl.util.Dimension;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.replaymod.editor.ReplayModEditor.LOGGER;
import static java.util.Optional.ofNullable;

public class GuiTrimPanel extends GuiPanel {
    // Special value indicating no replay files were found
    private static final File NO_REPLAY = new File(".");
    // Special value for the initial "Select Marker Keyframe" entry
    private static final Marker NO_MARKER = new Marker(); static {NO_MARKER.setTime(-1);}

    private final GuiReplayEditor gui;

    public final GuiDropdownMenu<File> inputReplays = new GuiDropdownMenu<File>(this)
            .setMinSize(new Dimension(200, 20)).onSelection(i -> updateSelectedReplay())
            .setToString(f -> f == NO_REPLAY ? "" : FilenameUtils.getBaseName(f.getName()));

    public final GuiNumberField startHour = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField startMin = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField startSec = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField startMilli = new GuiNumberField().setSize(40, 20).setMaxLength(4);
    public final GuiDropdownMenu<Marker> startMarker = new GuiDropdownMenu<>();

    public final GuiNumberField endHour = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField endMin = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField endSec = new GuiNumberField().setSize(20, 20).setMaxLength(2);
    public final GuiNumberField endMilli = new GuiNumberField().setSize(40, 20).setMaxLength(4);
    public final GuiDropdownMenu<Marker> endMarker = new GuiDropdownMenu<>();

    public final GuiPanel timePanel = new GuiPanel(this)
            .setLayout(new GridLayout().setCellsEqualSize(false).setSpacingX(20).setSpacingY(5).setColumns(3))
            .addElements(new GridLayout.Data(0, 0.5),
                    new GuiLabel().setI18nText("replaymod.gui.start"),
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(2)).addElements(
                            new HorizontalLayout.Data(0.5),
                            startHour, new GuiLabel().setI18nText("replaymod.gui.hours"),
                            startMin, new GuiLabel().setI18nText("replaymod.gui.minutes"),
                            startSec, new GuiLabel().setI18nText("replaymod.gui.seconds"),
                            startMilli, new GuiLabel().setI18nText("replaymod.gui.milliseconds")
                    ), startMarker,
                    new GuiLabel().setI18nText("replaymod.gui.end"),
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(2)).addElements(
                            new HorizontalLayout.Data(0.5),
                            endHour, new GuiLabel().setI18nText("replaymod.gui.hours"),
                            endMin, new GuiLabel().setI18nText("replaymod.gui.minutes"),
                            endSec, new GuiLabel().setI18nText("replaymod.gui.seconds"),
                            endMilli, new GuiLabel().setI18nText("replaymod.gui.milliseconds")
                    ), endMarker);

    public GuiTrimPanel(GuiReplayEditor gui) {
        this.gui = gui;

        setLayout(new VerticalLayout().setSpacing(10));

        Function<Marker, String> toString = m -> {
            if (m == NO_MARKER) {
                return I18n.format("replaymod.gui.editor.trim.marker");
            } else {
                return ofNullable(m.getName()).orElse(I18n.format("replaymod.gui.ingame.unnamedmarker"))
                        + " (" + Utils.convertSecondsToShortString(m.getTime() / 1000) + ")";
            }
        };
        startMarker.setToString(toString).setMinSize(new Dimension(100, 20)).onSelection(i ->
                onSelectedMarkerChanged(startMarker, startHour, startMin, startSec, startMilli));
        endMarker.setToString(toString).setMinSize(new Dimension(100, 20)).onSelection(i ->
                onSelectedMarkerChanged(endMarker, endHour, endMin, endSec, endMilli));

        File[] files = null;
        try {
            File folder = gui.getMod().getReplayFolder();
            files = folder.listFiles((FileFilter) new SuffixFileFilter(".mcpr", IOCase.INSENSITIVE));
        } catch (IOException e) {
            Utils.error(LOGGER, gui, CrashReport.makeCrashReport(e, "Listing available replays"), gui.backButton::onClick);
        }
        if (files == null) {
            LOGGER.warn("Replay file list is null, has the replay folder been deleted?");
            files = new File[0];
        } else {
            LOGGER.debug("Found {} replays in the replay folder", files.length);
        }
        if (files.length == 0) {
            inputReplays.setValues(NO_REPLAY);
        } else {
            inputReplays.setValues(files);
        }

        updateSelectedReplay();
        updateReadyState();

        gui.saveButton.onClick(() -> {
            // Read start and end times
            int start = ((startHour.getInteger() * 60 + startMin.getInteger()) * 60 + startSec.getInteger()) * 1000 + startMilli.getInteger();
            int end = ((endHour.getInteger() * 60 + endMin.getInteger()) * 60 + endSec.getInteger()) * 1000 + endMilli.getInteger();
            // Configure offset of change timestamp filter
            ChangeTimestampFilter ctf = new ChangeTimestampFilter();
            JsonObject config = new JsonObject();
            config.addProperty("offset", -start);
            ctf.init(null, config);
            // Pass filters to save dialog
            gui.save(inputReplays.getSelectedValue(),
                    new PacketStream.FilterInfo(new SquashFilter(), -1, start),
                    new PacketStream.FilterInfo(ctf, start, end),
                    new PacketStream.FilterInfo(new RemoveFilter(), end, -1)
            );
        });
    }

    private void onSelectedMarkerChanged(GuiDropdownMenu<Marker> dropdown, GuiNumberField hour, GuiNumberField min,
                                         GuiNumberField sec, GuiNumberField milli) {
        Marker marker = dropdown.getSelectedValue();
        if (marker != NO_MARKER) {
            int time = marker.getTime();
            setTime(time, hour, min, sec, milli);
        }
    }

    private void setTime(int time, GuiNumberField hour, GuiNumberField min,
                                         GuiNumberField sec, GuiNumberField milli) {
        milli.setValue(time % 1000); time /= 1000;
        sec.setValue(time % 60); time /= 60;
        min.setValue(time % 60); time /= 60;
        hour.setValue(time);
    }

    private void updateSelectedReplay() {
        File file = inputReplays.getSelectedValue();
        // Load markers and meta data from replay file
        ReplayMetaData metaData;
        Set<Marker> markers;
        if (file == NO_REPLAY) {
            metaData = new ReplayMetaData();
            markers = Collections.emptySet();
        } else {
            try (ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), file)) {
                metaData = replayFile.getMetaData();
                markers = replayFile.getMarkers().or(Collections.emptySet());
                LOGGER.debug("Loaded {} markers from replay {}", markers.size(), file);
            } catch (Exception e) {
                Utils.error(LOGGER, gui, CrashReport.makeCrashReport(e, "Reading markers from replay"), null);
                metaData = new ReplayMetaData();
                markers = Collections.emptySet();
            }
        }

        // Sort and add initial "Select Event Marker" element
        List<Marker> markerList = new ArrayList<>(markers);
        markerList.sort(Comparator.comparing(Marker::getTime));
        markerList.add(0, NO_MARKER);

        // Set marker dropdown values
        Marker[] markerArray = markerList.stream().toArray(Marker[]::new);
        startMarker.setValues(markerArray).setSelected(0);
        endMarker.setValues(markerArray).setSelected(0);

        // Set start and end time values
        setTime(0, startHour, startMin, startSec, startMilli);
        setTime(metaData.getDuration(), endHour, endMin, endSec, endMilli);
    }

    private void updateReadyState() {
        gui.saveButton.setEnabled(inputReplays.getSelectedValue() != NO_REPLAY);
    }
}
