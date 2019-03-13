package com.replaymod.editor.gui;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.filter.SquashFilter;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.IteratorStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.util.Utils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            return inputReplayFile.getMarkers().or(HashSet::new).stream().anyMatch(m -> m.getName().startsWith("_RM_"));
        }
    }

    public static void apply(Path path, Consumer<Float> progress) throws IOException {
        if (!hasWork(path)) {
            return;
        }

        String replayName = FilenameUtils.getBaseName(path.getFileName().toString());
        int splitCounter = 0;

        Studio studio = new ReplayStudio();
        SquashFilter squashFilter = new SquashFilter();
        squashFilter.init(studio, null);

        Path inputPath = path.resolveSibling("raw").resolve(path.getFileName());
        Files.createDirectories(inputPath.getParent());
        Files.move(path, inputPath);

        try (ZipReplayFile inputReplayFile = new ZipReplayFile(studio, inputPath.toFile())) {
            List<Marker> markers = inputReplayFile.getMarkers().or(HashSet::new)
                    .stream().sorted(Comparator.comparing(Marker::getTime)).collect(Collectors.toList());
            Iterator<Marker> markerIterator = markers.iterator();
            boolean anySplit = markers.stream().anyMatch(m -> m.getName().equals(MARKER_NAME_SPLIT));

            int inputDuration = inputReplayFile.getMetaData().getDuration();
            ReplayInputStream replayInputStream = inputReplayFile.getPacketData(studio, true);
            int timeOffset = 0;
            SquashFilter cutFilter = null;
            int startCutOffset = 0;
            PacketData nextPacket = replayInputStream.readPacket();
            Marker nextMarker = markerIterator.next();

            while (nextPacket != null) {
                try (ZipReplayFile outputReplayFile = new ZipReplayFile(studio, null,
                        (anySplit ? path.resolveSibling(replayName + "_" + splitCounter + ".mcpr") : path).toFile())) {
                    long duration = 0;
                    Set<Marker> outputMarkers = new HashSet<>();
                    ReplayMetaData metaData = inputReplayFile.getMetaData();
                    metaData.setDate(metaData.getDate() + timeOffset);

                    try (ReplayOutputStream replayOutputStream = outputReplayFile.writePacketData(true)) {
                        if (cutFilter != null) {
                            cutFilter = squashFilter;
                        } else if (splitCounter > 0) {
                            List<PacketData> packets = new ArrayList<>();
                            squashFilter.onEnd(
                                    new IteratorStream(packets.listIterator(), (StreamFilter) null),
                                    timeOffset
                            );
                            for (PacketData packet : packets) {
                                replayOutputStream.write(0, packet.getPacket());
                            }
                        }

                        while (nextPacket != null) {
                            if (nextMarker != null && nextPacket.getTime() > nextMarker.getTime()) {
                                if (MARKER_NAME_START_CUT.equals(nextMarker.getName())) {
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

                            squashFilter.onPacket(null, nextPacket);
                            if (cutFilter != null) {
                                if (cutFilter != squashFilter) {
                                    cutFilter.onPacket(null, nextPacket);
                                }
                            } else {
                                replayOutputStream.write(nextPacket.getTime() - timeOffset, nextPacket.getPacket());
                                duration = nextPacket.getTime() - timeOffset;
                            }
                            nextPacket = replayInputStream.readPacket();
                            if (nextPacket != null) {
                                progress.accept((float) nextPacket.getTime() / (float) inputDuration);
                            } else {
                                progress.accept(1f);
                            }
                        }
                    }

                    metaData.setDuration((int) duration);
                    outputReplayFile.writeMetaData(metaData);

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
                }
            }
        }
    }
}
