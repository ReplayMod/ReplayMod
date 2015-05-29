package eu.crushedpixel.replaymod.studio;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.ChangeTimestampFilter;
import de.johni0702.replaystudio.filter.RemoveFilter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.studio.ReplayStudio;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudioImplementation {

    public static void trimReplay(File replayFile, boolean isTmcpr, int beginning, int ending, File outputFile) throws IOException {
        ReplayStudio studio = new ReplayStudio();
        studio.setWrappingEnabled(false);
        PacketStream stream = studio.createReplayStream(new FileInputStream(replayFile), isTmcpr);
        stream.addFilter(new SquashFilter(), -1, beginning);
        stream.addFilter(new RemoveFilter(), ending, -1);

        ChangeTimestampFilter ctf = new ChangeTimestampFilter();
        JsonObject cfg = new JsonObject();
        cfg.addProperty("offset", -beginning);
        ctf.init(studio, cfg);

        stream.addFilter(ctf, beginning, ending);

        File temp = File.createTempFile("trimmed", "tmcpr");
        temp.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(temp);
        ReplayOutputStream out = new ReplayOutputStream(studio, fos);
        PacketData packetData;

        while((packetData = stream.next()) != null) {
            out.write(packetData);
        }
        for(PacketData data : stream.end()) {
            out.write(data);
        }

        out.close();

        ReplayFile replay = new ReplayFile(replayFile);
        ReplayMetaData metaData = replay.metadata().get();
        ending = Math.min(metaData.getDuration(), ending);
        metaData.setDuration(ending - beginning);

        Map<Integer, String> resourcePackIndex = replay.resourcePackIndex().get();
        Map<String, File> resourcePacks = new HashMap<String, File>();
        File tempFolder = Files.createTempDir();
        for (String hash : resourcePackIndex.values()) {
            if (!resourcePacks.containsKey(hash)) {
                File resourcePack = new File(tempFolder, hash);
                IOUtils.copy(replay.resourcePack(hash).get(), new FileOutputStream(resourcePack));
                resourcePacks.put(hash, resourcePack);
            }
        }

        outputFile.createNewFile();

        ReplayFileIO.writeReplayFile(outputFile, temp, metaData, resourcePacks, resourcePackIndex);
    }

    //TODO Work with Johni to connect multiple Replay Files
    public static void connectReplayFiles(List<File> filesToConnect, File outputFile) {

    }
}
