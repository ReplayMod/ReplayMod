package eu.crushedpixel.replaymod.studio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.RemoveFilter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.studio.ReplayStudio;

public class StudioImplementation {
	
	public static void trimReplay(InputStream replayFileStream, boolean isTmcpr, int beginning, int ending, File outputFile) throws IOException {
		ReplayStudio studio = new ReplayStudio();
        PacketStream stream = studio.createReplayStream(replayFileStream, isTmcpr);
        stream.addFilter(new SquashFilter(), -1, beginning);
        stream.addFilter(new RemoveFilter(), ending, -1);
        
        outputFile.createNewFile();
        
        FileOutputStream fos = new FileOutputStream(outputFile);
        ReplayOutputStream out = new ReplayOutputStream(studio, fos);
        PacketData packetData;
        
        while((packetData = stream.next()) != null) {
            out.write(packetData);
        }
        for(PacketData data : stream.end()) {
            out.write(data);
        }
        out.close();
	}
}
