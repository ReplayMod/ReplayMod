package com.replaymod.render;

import com.google.common.base.Optional;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.simplepathing.SPTimeline;
import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplayModRender extends EventRegistrations implements Module {
    { instance = this; }
    public static ReplayModRender instance;

    private ReplayMod core;

    public static Logger LOGGER = LogManager.getLogger();

    private ReplayFile replayFile;
    private final List<RenderJob> renderQueue = new ArrayList<>();

    public ReplayModRender(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    public ReplayMod getCore() {
        return core;
    }

    @Override
    public void initClient() {
        register();
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File folder = new File(path.startsWith("./") ? core.getMinecraft().runDirectory : null, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new CrashException(CrashReport.create(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Path getRenderSettingsPath() {
        return core.getMinecraft().runDirectory.toPath().resolve("config/replaymod-rendersettings.json");
    }

    public List<RenderJob> getRenderQueue() {
        return renderQueue;
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpened); }
    private void onReplayOpened(ReplayHandler replayHandler) {
        replayFile = replayHandler.getReplayFile();
        try {
            renderQueue.addAll(doLoadRenderQueue(replayFile));
        } catch (IOException e) {
            throw new CrashException(CrashReport.create(e, "Reading timeline"));
        }
    }

    { on(ReplayClosedCallback.EVENT, replayHandler -> onReplayClosed()); }
    private void onReplayClosed() {
        renderQueue.clear();
        replayFile = null;
    }

    private static List<RenderJob> doLoadRenderQueue(ReplayFile replayFile) throws IOException {
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

    private static void doSaveRenderQueue(ReplayFile replayFile, List<RenderJob> renderQueue) throws IOException {
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

    public void saveRenderQueue() {
        try {
            doSaveRenderQueue(replayFile, renderQueue);
        } catch (IOException e) {
            e.printStackTrace();
            VanillaGuiScreen screen = VanillaGuiScreen.setup(getCore().getMinecraft().currentScreen);
            CrashReport report = CrashReport.create(e, "Reading timeline");
            Utils.error(LOGGER, screen, report, () -> {});
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
