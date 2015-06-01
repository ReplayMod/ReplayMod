package eu.crushedpixel.replaymod.studio;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.ChangeTimestampFilter;
import de.johni0702.replaystudio.filter.RemoveFilter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.studio.ReplayStudio;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StudioImplementation {

    public static void trimReplay(File file, int beginning, int ending, File outputFile) throws IOException {
        ReplayStudio studio = new ReplayStudio();
        studio.setWrappingEnabled(false);
        ReplayFile replayFile = new ZipReplayFile(studio, file);

        PacketStream stream = studio.createReplayStream(replayFile.getPacketData(), true);
        stream.addFilter(new SquashFilter(), -1, beginning);
        stream.addFilter(new RemoveFilter(), ending, -1);

        ChangeTimestampFilter ctf = new ChangeTimestampFilter();
        JsonObject cfg = new JsonObject();
        cfg.addProperty("offset", -beginning);
        ctf.init(studio, cfg);

        stream.addFilter(ctf, beginning, ending);

        ReplayOutputStream out = replayFile.writePacketData();
        PacketData packetData;

        while((packetData = stream.next()) != null) {
            out.write(packetData);
        }
        for(PacketData data : stream.end()) {
            out.write(data);
        }

        out.close();

        ReplayMetaData metaData = replayFile.getMetaData();
        ending = Math.min(metaData.getDuration(), ending);
        metaData.setDuration(ending - beginning);
        replayFile.writeMetaData(metaData);

        replayFile.saveTo(outputFile);
    }

    //TODO Work with Johni to connect multiple Replay Files
    public static void connectReplayFiles(List<File> filesToConnect, File outputFile) {

    }
}
