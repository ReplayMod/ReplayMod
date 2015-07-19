package eu.crushedpixel.replaymod.studio;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.filter.StreamFilter;
import de.johni0702.replaystudio.stream.PacketStream;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;

public class ProgressFilter implements StreamFilter {

    private final long total;

    private ProgressUpdateListener listener;
    private float minPerc, maxPerc;

    public ProgressFilter(long total, ProgressUpdateListener progressUpdateListener, float minPerc, float maxPerc) {
        this.total = total;

        this.listener = progressUpdateListener;
        this.minPerc = minPerc;
        this.maxPerc = maxPerc;
    }

    @Override
    public String getName() {
        return "progress";
    }

    @Override
    public void init(Studio studio, JsonObject config) {}

    @Override
    public void onStart(PacketStream stream) {}

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        float percentage = (data.getTime() / (float)total);

        float perc = minPerc+(percentage*(maxPerc-minPerc));

        this.listener.onProgressChanged(perc);

        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {}
}
