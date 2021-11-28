package com.replaymod.render.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.render.RenderSettings;
import com.replaymod.replaystudio.lib.guava.base.Optional;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.simplepathing.SPTimeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RenderJob {
    private Timeline timeline;
    private RenderSettings settings;

    public RenderJob() {
    }

    public String getName() {
        return settings.getOutputFile().getName();
    }

    public Timeline getTimeline() {
        return this.timeline;
    }

    public RenderSettings getSettings() {
        return this.settings;
    }

    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    public void setSettings(RenderSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RenderJob renderJob = (RenderJob) o;
        return timeline.equals(renderJob.timeline) &&
                settings.equals(renderJob.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeline, settings);
    }

    @Override
    public String toString() {
        return "RenderJob{" +
                "timeline=" + timeline +
                ", settings=" + settings +
                '}';
    }

    public static List<RenderJob> readQueue(ReplayFile replayFile) throws IOException {
        synchronized (replayFile) {
            Optional<InputStream> optIn = replayFile.get("renderQueue.json");
            if (!optIn.isPresent()) {
                return new ArrayList<>();
            }
            try (InputStream in = optIn.get();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new GsonBuilder()
                        .registerTypeAdapter(Timeline.class, new TimelineTypeAdapter())
                        .create()
                        .fromJson(reader, new TypeToken<List<RenderJob>>(){}.getType());
            }
        }
    }

    public static void writeQueue(ReplayFile replayFile, List<RenderJob> renderQueue) throws IOException {
        synchronized (replayFile) {
            try (OutputStream out = replayFile.write("renderQueue.json");
                 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                new GsonBuilder()
                        .registerTypeAdapter(Timeline.class, new TimelineTypeAdapter())
                        .create()
                        .toJson(renderQueue, writer);
            }
        }
    }

    private static class TimelineTypeAdapter extends TypeAdapter<Timeline> {

        private final TimelineSerialization serialization;

        public TimelineTypeAdapter(TimelineSerialization serialization) {
            this.serialization = serialization;
        }

        public TimelineTypeAdapter(PathingRegistry registry) {
            this(new TimelineSerialization(registry, null));
        }

        public TimelineTypeAdapter() {
            // TODO need to somehow get rid of the reliance on simplepathing
            this(new SPTimeline());
        }

        @Override
        public void write(JsonWriter out, Timeline value) throws IOException {
            out.value(serialization.serialize(Collections.singletonMap("", value)));
        }

        @Override
        public Timeline read(JsonReader in) throws IOException {
            return serialization.deserialize(in.nextString()).get("");
        }
    }
}
