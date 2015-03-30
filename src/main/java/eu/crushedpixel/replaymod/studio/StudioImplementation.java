package eu.crushedpixel.replaymod.studio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.gson.JsonObject;

import net.minecraft.network.play.server.S3EPacketTeams;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.ChangeTimestampFilter;
import de.johni0702.replaystudio.filter.RemoveFilter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.studio.ReplayStudio;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

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
        
        ReplayMetaData metaData = ReplayFileIO.getMetaData(replayFile);
        ending = Math.min(metaData.getDuration(), ending);
        metaData.setDuration(ending-beginning);
        
        outputFile.createNewFile();
        
        ReplayFileIO.writeReplayFile(outputFile, temp, metaData);
	}
}
