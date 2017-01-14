package com.replaymod.render.utils;

import com.replaymod.render.RenderSettings;
import com.replaymod.replaystudio.pathing.path.Timeline;
import lombok.Data;

@Data
public class RenderJob {
    private String name;
    private Timeline timeline;
    private RenderSettings settings;
}
