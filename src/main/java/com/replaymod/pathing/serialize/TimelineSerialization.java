package com.replaymod.pathing.serialize;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.replaymod.pathing.PathingRegistry;
import com.replaymod.pathing.interpolation.Interpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.PathSegment;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.property.Property;
import de.johni0702.replaystudio.replay.ReplayFile;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

public class TimelineSerialization {
    private static final String FILE_ENTRY = "timelines.json";

    private final PathingRegistry registry;
    private final ReplayFile replayFile;

    public TimelineSerialization(PathingRegistry registry, ReplayFile replayFile) {
        this.registry = registry;
        this.replayFile = replayFile;
    }

    public void save(Map<String, Timeline> timelines) throws IOException {
        String serialized = serialize(timelines);
        try (OutputStream out = replayFile.write(FILE_ENTRY)) {
            out.write(serialized.getBytes(Charsets.UTF_8));
        }
    }

    public Map<String, Timeline> load() throws IOException {
        Map<String, Timeline> timelines = LegacyTimelineConverter.convert(registry, replayFile);

        Optional<InputStream> optionalIn = replayFile.get(FILE_ENTRY);
        if (optionalIn.isPresent()) {
            String serialized;
            try (InputStream in = optionalIn.get()) {
                serialized = IOUtils.toString(in, Charsets.UTF_8);
            }
            Map<String, Timeline> deserialized = deserialize(serialized);
            timelines.putAll(deserialized);
        }
        return timelines;
    }

    public String serialize(Map<String, Timeline> timelines) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        writer.beginObject();
        for (Map.Entry<String, Timeline> entry : timelines.entrySet()) {
            Timeline timeline = entry.getValue();
            writer.name(entry.getKey()).beginArray();
            for (Path path : timeline.getPaths()) {
                writer.beginObject();
                writer.name("keyframes").beginArray();
                for (Keyframe keyframe : path.getKeyframes()) {
                    writer.beginObject();
                    writer.name("time").value(keyframe.getTime());
                    writer.name("properties").beginObject();
                    for (Property<?> property : keyframe.getProperties()) {
                        writer.name((property.getGroup() == null ? "" : property.getGroup().getId() + ":") + property.getId());
                        writeProperty(writer, keyframe, property);
                    }
                    writer.endObject();
                    writer.endObject();
                }
                writer.endArray();
                Map<Interpolator, Integer> interpolators = new LinkedHashMap<>();
                writer.name("segments").beginArray();
                for (PathSegment segment : path.getSegments()) {
                    Interpolator interpolator = segment.getInterpolator();
                    if (interpolator == null) {
                        writer.nullValue();
                    } else {
                        Integer index = interpolators.get(interpolator);
                        if (index == null) {
                            interpolators.put(interpolator, index = interpolators.size());
                        }
                        writer.value(index);
                    }
                }
                writer.endArray();
                writer.name("interpolators").beginArray();
                for (Interpolator interpolator : interpolators.keySet()) {
                    registry.serializeInterpolator(writer, interpolator);
                }
                writer.endArray();
                writer.endObject();
            }
            writer.endArray();
        }
        writer.endObject();
        writer.flush();
        return stringWriter.toString();
    }

    private static <T> void writeProperty(JsonWriter writer, Keyframe keyframe, Property<T> property) throws IOException {
        property.toJson(writer, keyframe.getValue(property).get());
    }

    public Map<String, Timeline> deserialize(String serialized) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(serialized));
        Map<String, Timeline> timelines = new LinkedHashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            Timeline timeline = registry.createTimeline();
            timelines.put(reader.nextName(), timeline);
            reader.beginArray();
            while (reader.hasNext()) {
                Path path = timeline.createPath();
                reader.beginObject();
                List<Integer> segments = new ArrayList<>();
                List<Interpolator> interpolators = new ArrayList<>();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                        case "keyframes":
                            reader.beginArray();
                            while (reader.hasNext()) {
                                long time = 0;
                                Map<Property, Object> properties = new HashMap<>();
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    switch (reader.nextName()) {
                                        case "time":
                                            time = reader.nextLong();
                                            break;
                                        case "properties":
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                String id = reader.nextName();
                                                Property property = timeline.getProperty(id);
                                                if (property == null) {
                                                    throw new IOException("Unknown property: " + id);
                                                }
                                                Object value = property.fromJson(reader);
                                                properties.put(property, value);
                                            }
                                            reader.endObject();
                                            break;
                                    }
                                }
                                reader.endObject();
                                Keyframe keyframe = path.insert(time);
                                for (Map.Entry<Property, Object> entry : properties.entrySet()) {
                                    keyframe.setValue(entry.getKey(), entry.getValue());
                                }
                            }
                            reader.endArray();
                            break;
                        case "segments":
                            reader.beginArray();
                            while (reader.hasNext()) {
                                if (reader.peek() == JsonToken.NULL) {
                                    reader.nextNull();
                                    segments.add(null);
                                } else {
                                    segments.add(reader.nextInt());
                                }
                            }
                            reader.endArray();
                            break;
                        case "interpolators":
                            reader.beginArray();
                            while (reader.hasNext()) {
                                interpolators.add(registry.deserializeInterpolator(reader));
                            }
                            reader.endArray();
                            break;
                    }
                }
                Iterator<Integer> iter = segments.iterator();
                for (PathSegment segment : path.getSegments()) {
                    Integer next = iter.next();
                    if (next != null) {
                        segment.setInterpolator(interpolators.get(next));
                    }
                }
                reader.endObject();
            }
            reader.endArray();
        }
        reader.endObject();
        return timelines;
    }
}
