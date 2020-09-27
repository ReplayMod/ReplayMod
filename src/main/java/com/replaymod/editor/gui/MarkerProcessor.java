package com.replaymod.editor.gui;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.filter.SquashFilter;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.IteratorStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.replaystudio.util.Utils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MarkerProcessor {
    /**
     * Signals the start of a section which is to be cut from the replay.
     */
    public static final String MARKER_NAME_START_CUT = "_RM_START_CUT";
    /**
     * Signals the end of a section which is to be cut from the replay.
     */
    public static final String MARKER_NAME_END_CUT = "_RM_END_CUT";
    /**
     * Signals the point at which a replay is to be split into multiple replays.
     * If it is found in a section which is to be cut, that section will be cut from both replays.
     */
    public static final String MARKER_NAME_SPLIT = "_RM_SPLIT";

    private static boolean hasWork(Path path) throws IOException {
        try (ZipReplayFile inputReplayFile = new ZipReplayFile(new ReplayStudio(), path.toFile())) {
            return inputReplayFile.getMarkers().or(HashSet::new).stream().anyMatch(m -> m.getName() != null && m.getName().startsWith("_RM_"));
        }
    }

    private enum OutputState {
        /** A new output file has begun but not data has been written yet. */
        NotYetWriting,
        /** Currently writing data to the active output file. */
        Writing,
        /** Currently not writing data. */
        Paused,
    }

    private static List<String> getOutputSuffixes(ReplayFile inputReplayFile) throws IOException {
        List<Marker> markers = inputReplayFile.getMarkers().or(HashSet::new)
                .stream().sorted(Comparator.comparing(Marker::getTime)).collect(Collectors.toList());
        int nextSuffix = 0;
        List<String> suffixes = new ArrayList<>();
        OutputState state = OutputState.Writing;
        for (Marker marker : markers) {
            if (MARKER_NAME_START_CUT.equals(marker.getName())) {
                if (marker.getTime() == 0) {
                    // Special case: Automatic recording has been disabled
                    state = OutputState.NotYetWriting;
                } else {
                    state = OutputState.Paused;
                }
            } else if (MARKER_NAME_END_CUT.equals(marker.getName())) {
                state = OutputState.Writing;
            } else if (MARKER_NAME_SPLIT.equals(marker.getName())) {
                switch (state) {
                    case NotYetWriting:
                        break;
                    case Writing:
                        suffixes.add("_" + nextSuffix++);
                        state = OutputState.Writing;
                        break;
                    case Paused:
                        suffixes.add("_" + nextSuffix++);
                        state = OutputState.NotYetWriting;
                        break;
                }
            }
        }
        if (state != OutputState.NotYetWriting) {
            suffixes.add("_" + nextSuffix);
        }

        if (suffixes.size() == 1) {
            // If there's only one output, it can keep the name of the input
            return Collections.singletonList("");
        }
        return suffixes;
    }

    public static List<Pair<Path, ReplayMetaData>> apply(Path path, Consumer<Float> progress) throws IOException {
        if (!hasWork(path)) {
            ReplayMetaData metaData;
            try (ZipReplayFile inputReplayFile = new ZipReplayFile(new ReplayStudio(), path.toFile())) {
                metaData = inputReplayFile.getMetaData();
            }
            return Collections.singletonList(new Pair<>(path, metaData));
        }

        String replayName = FilenameUtils.getBaseName(path.getFileName().toString());
        int splitCounter = 0;

        PacketTypeRegistry registry = MCVer.getPacketTypeRegistry(true);
        Studio studio = new ReplayStudio();
        SquashFilter squashFilter = new SquashFilter();
        squashFilter.init(studio, null);

        List<Pair<Path, ReplayMetaData>> outputPaths = new ArrayList<>();

        Path inputPath = path.resolveSibling("raw").resolve(path.getFileName());
        for (int i = 1; Files.exists(inputPath); i++) {
            inputPath = inputPath.resolveSibling(replayName + "." + i + ".mcpr");
        }
        Files.createDirectories(inputPath.getParent());
        Files.move(path, inputPath);

        try (ZipReplayFile inputReplayFile = new ZipReplayFile(studio, inputPath.toFile())) {
            List<Marker> markers = inputReplayFile.getMarkers().or(HashSet::new)
                    .stream().sorted(Comparator.comparing(Marker::getTime)).collect(Collectors.toList());
            Iterator<Marker> markerIterator = markers.iterator();
            Iterator<String> outputFileSuffixes = getOutputSuffixes(inputReplayFile).iterator();
            boolean anySplit = markers.stream().anyMatch(m -> MARKER_NAME_SPLIT.equals(m.getName()));

            int inputDuration = inputReplayFile.getMetaData().getDuration();
            ReplayInputStream replayInputStream = inputReplayFile.getPacketData(registry);
            int timeOffset = 0;
            SquashFilter cutFilter = null;
            int startCutOffset = 0;
            PacketData nextPacket = replayInputStream.readPacket();
            Marker nextMarker = markerIterator.next();

            while (nextPacket != null && outputFileSuffixes.hasNext()) {
                Path outputPath = path.resolveSibling(replayName + outputFileSuffixes.next() + ".mcpr");
                File outputFile = outputPath.toFile();
                try (ZipReplayFile outputReplayFile = new ZipReplayFile(studio, null, outputFile)) {
                    long duration = 0;
                    Set<Marker> outputMarkers = new HashSet<>();
                    ReplayMetaData metaData = inputReplayFile.getMetaData();
                    metaData.setDate(metaData.getDate() + timeOffset);

                    try (ReplayOutputStream replayOutputStream = outputReplayFile.writePacketData()) {
                        if (cutFilter != null) {
                            cutFilter.release();
                            cutFilter = squashFilter.copy();
                        } else if (splitCounter > 0) {
                            List<PacketData> packets = new ArrayList<>();
                            squashFilter.copy().onEnd(
                                    new IteratorStream(packets.listIterator(), (StreamFilter) null),
                                    timeOffset
                            );
                            for (PacketData packet : packets) {
                                replayOutputStream.write(0, packet.getPacket());
                            }
                        }
                        boolean hasFurtherOutputs = outputFileSuffixes.hasNext();

                        while (nextPacket != null) {
                            if (nextMarker != null && nextPacket.getTime() >= nextMarker.getTime()) {
                                if (MARKER_NAME_START_CUT.equals(nextMarker.getName())) {
                                    if (cutFilter != null) {
                                        cutFilter.release();
                                    }
                                    startCutOffset = nextMarker.getTime();
                                    cutFilter = new SquashFilter();
                                } else if (MARKER_NAME_END_CUT.equals(nextMarker.getName())) {
                                    timeOffset += nextMarker.getTime() - startCutOffset;
                                    if (cutFilter != null) {
                                        List<PacketData> packets = new ArrayList<>();
                                        cutFilter.onEnd(
                                                new IteratorStream(packets.listIterator(), (StreamFilter) null),
                                                nextMarker.getTime()
                                        );
                                        for (PacketData packet : packets) {
                                            replayOutputStream.write(nextMarker.getTime() - timeOffset, packet.getPacket());
                                        }
                                        cutFilter = null;
                                    }
                                } else if (MARKER_NAME_SPLIT.equals(nextMarker.getName())) {
                                    splitCounter++;
                                    timeOffset = nextMarker.getTime();
                                    startCutOffset = timeOffset;
                                    nextMarker = markerIterator.hasNext() ? markerIterator.next() : null;
                                    break;
                                } else {
                                    nextMarker.setTime(nextMarker.getTime() - timeOffset);
                                    outputMarkers.add(nextMarker);
                                }
                                nextMarker = markerIterator.hasNext() ? markerIterator.next() : null;
                                continue;
                            }

                            if (hasFurtherOutputs) {
                                squashFilter.onPacket(null, nextPacket);
                            }
                            if (cutFilter != null) {
                                cutFilter.onPacket(null, nextPacket);
                            } else {
                                replayOutputStream.write(nextPacket.getTime() - timeOffset, nextPacket.getPacket().copy());
                                duration = nextPacket.getTime() - timeOffset;
                            }
                            nextPacket.release();
                            nextPacket = replayInputStream.readPacket();
                            if (nextPacket != null) {
                                progress.accept((float) nextPacket.getTime() / (float) inputDuration);
                            } else {
                                progress.accept(1f);
                            }
                        }
                    }

                    metaData.setDuration((int) duration);
                    outputReplayFile.writeMetaData(registry, metaData);

                    outputReplayFile.writeMarkers(outputMarkers);

                    outputReplayFile.writeModInfo(inputReplayFile.getModInfo());

                    // We could filter these but ReplayStudio doesn't yet provide a nice method for determining
                    // which ones are used.
                    Map<Integer, String> resourcePackIndex = inputReplayFile.getResourcePackIndex();
                    if (resourcePackIndex != null) {
                        outputReplayFile.writeResourcePackIndex(resourcePackIndex);
                        for (String hash : resourcePackIndex.values()) {
                            try (InputStream in = inputReplayFile.getResourcePack(hash).get();
                                 OutputStream out = outputReplayFile.writeResourcePack(hash)) {
                                Utils.copy(in, out);
                            }
                        }
                    }

                    outputReplayFile.save();

                    outputPaths.add(new Pair(outputPath, metaData));
                }
            }

            squashFilter.release();
            if (cutFilter != null) {
                cutFilter.release();
            }
        }

        return outputPaths;
    }
}
