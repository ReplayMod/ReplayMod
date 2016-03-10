package com.replaymod.pathing.serialize;

import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import com.replaymod.pathing.PathingRegistry;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import de.johni0702.replaystudio.replay.ReplayFile;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class LegacyTimelineConverter {
    public static Map<String, Timeline> convert(PathingRegistry registry, ReplayFile replayFile) throws IOException {
        KeyframeSet[] keyframeSets = readAndParse(replayFile);
        if (keyframeSets == null) {
            return Collections.emptyMap();
        }

        Map<String, Timeline> timelines = new LinkedHashMap<>();
        for (KeyframeSet keyframeSet : keyframeSets) {
            timelines.put(keyframeSet.name, convert(registry, keyframeSet));
        }
        return timelines;
    }

    private static Optional<InputStream> read(ReplayFile replayFile) throws IOException {
        Optional<InputStream> in = replayFile.get("paths.json");
        if (!in.isPresent()) {
            in = replayFile.get("paths");
        }
        return in;
    }

    private static KeyframeSet[] parse(InputStream in) {
        return new GsonBuilder()
                .registerTypeAdapter(KeyframeSet[].class, new LegacyKeyframeSetAdapter())
                .create().fromJson(new InputStreamReader(in), KeyframeSet[].class);
    }

    private static KeyframeSet[] readAndParse(ReplayFile replayFile) throws IOException {
        Optional<InputStream> optIn = read(replayFile);
        if (!optIn.isPresent()) {
            return null;
        }
        KeyframeSet[] keyframeSets;
        try (InputStream in = optIn.get()) {
            keyframeSets = parse(in);
        }
        return keyframeSets;
    }

    private static Timeline convert(PathingRegistry registry, KeyframeSet keyframeSet) {
        Path path = registry.createTimeline().createPath();
        for (Keyframe<AdvancedPosition> positionKeyframe : keyframeSet.positionKeyframes) {
            AdvancedPosition value = positionKeyframe.value;
            com.replaymod.pathing.path.Keyframe keyframe = getKeyframe(path, positionKeyframe.realTimestamp);
            keyframe.setValue(CameraProperties.POSITION, Triple.of(value.x, value.y, value.z));
            keyframe.setValue(CameraProperties.ROTATION, Triple.of(value.yaw, value.pitch, value.roll));
            if (value instanceof SpectatorData) {
                // TODO Spectator keyframes
            }
        }
        for (Keyframe<TimestampValue> timeKeyframe : keyframeSet.timeKeyframes) {
            TimestampValue value = timeKeyframe.value;
            com.replaymod.pathing.path.Keyframe keyframe = getKeyframe(path, timeKeyframe.realTimestamp);
            keyframe.setValue(TimestampProperty.PROPERTY, (int) value.value);
        }
        return path.getTimeline();
    }

    private static com.replaymod.pathing.path.Keyframe getKeyframe(Path path, long time) {
        com.replaymod.pathing.path.Keyframe keyframe = path.getKeyframe(time);
        if (keyframe == null) {
            keyframe = path.insert(time);
        }
        return keyframe;
    }

    static class KeyframeSet {
        String name;
        Keyframe<AdvancedPosition>[] positionKeyframes;
        Keyframe<TimestampValue>[] timeKeyframes;
        CustomImageObject[] customObjects;
    }
    static class Keyframe<T> {
        int realTimestamp;
        T value;
    }
    static class Position {
        double x, y, z;
    }
    static class AdvancedPosition extends Position {
        float pitch, yaw, roll;
    }
    static class SpectatorData extends AdvancedPosition {
        Integer spectatedEntityID;
        SpectatingMethod spectatingMethod;
        SpectatorDataThirdPersonInfo thirdPersonInfo;
        enum SpectatingMethod {
            FIRST_PERSON, SHOULDER_CAM;
        }
    }
    static class SpectatorDataThirdPersonInfo {
        double shoulderCamDistance;
        double shoulderCamPitchOffset;
        double shoulderCamYawOffset;
        double shoulderCamSmoothness;
    }
    static class TimestampValue {
        double value;
    }
    static class CustomImageObject {
        String name;
        UUID linkedAsset;
        float width, height;
        float textureWidth, textureHeight;
        Transformations transformations = new Transformations();
    }
    static class Transformations {
        Position defaultAnchor, defaultPosition, defaultOrientation, defaultScale;
        NumberValue defaultOpacity;

        List<Position> anchorKeyframes;
        List<Position> positionKeyframes;
        List<Position> orientationKeyframes;
        List<Position> scaleKeyframes;
        List<NumberValue> opacityKeyframes;
    }
    static class NumberValue {
        double value;
    }
}
