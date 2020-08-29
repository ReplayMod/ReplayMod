package com.replaymod.render.utils;

import com.replaymod.render.RenderSettings;
import com.replaymod.replaystudio.pathing.path.Timeline;

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
}
