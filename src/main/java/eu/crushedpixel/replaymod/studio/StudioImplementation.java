package eu.crushedpixel.replaymod.studio;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.filter.ChangeTimestampFilter;
import de.johni0702.replaystudio.filter.NeutralizerFilter;
import de.johni0702.replaystudio.filter.RemoveFilter;
import de.johni0702.replaystudio.filter.SquashFilter;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.stream.PacketStream;
import de.johni0702.replaystudio.studio.ReplayStudio;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StudioImplementation {

    public static void trimReplay(File file, int beginning, int ending, File outputFile, ProgressUpdateListener updateListener) throws IOException {
        updateListener.onProgressChanged(0f, I18n.format("replaymod.gui.editor.progress.status.initializing"));

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

        updateListener.onProgressChanged(1/4f, I18n.format("replaymod.gui.editor.progress.status.writing.raw"));

        while((packetData = stream.next()) != null) {
            out.write(packetData);
        }

        List<PacketData> endList = stream.end();

        for(PacketData data : endList) {
            out.write(data);
        }

        out.close();

        updateListener.onProgressChanged(2/4f, I18n.format("replaymod.gui.editor.progress.status.writing.zip"));

        ReplayMetaData metaData = replayFile.getMetaData();
        ending = Math.min(metaData.getDuration(), ending);
        metaData.setDuration(ending - beginning);
        replayFile.writeMetaData(metaData);

        updateListener.onProgressChanged(3/4f, I18n.format("replaymod.gui.editor.progress.status.writing.final"));

        replayFile.saveTo(outputFile);

        updateListener.onProgressChanged(1f, I18n.format("replaymod.gui.editor.progress.status.finished"));
    }

    public static void connectReplayFiles(List<File> filesToConnect, File outputFile, ProgressUpdateListener updateListener) throws IOException {
        updateListener.onProgressChanged(0f, I18n.format("replaymod.gui.editor.progress.status.initializing"));

        Validate.notEmpty(filesToConnect);
        ReplayStudio studio = new ReplayStudio();
        studio.setWrappingEnabled(false);

        ReplayFile outputReplayFile = new ZipReplayFile(studio, outputFile);
        ReplayOutputStream out = outputReplayFile.writePacketData();

        ConnectMetadataFilter metaDataFilter = new ConnectMetadataFilter();
        int startTime = 0;

        updateListener.onProgressChanged(1/4f, I18n.format("replaymod.gui.editor.progress.status.writing.raw"));

        float i = 0;
        float size = filesToConnect.size();

        for (File file : filesToConnect) {
            ReplayFile replayFile = new ZipReplayFile(studio, file);
            PacketStream stream = studio.createReplayStream(replayFile.getPacketData(), true);
            ReplayMetaData metaData = replayFile.getMetaData();

            stream.addFilter(new NeutralizerFilter());

            if (startTime > 0) {
                ChangeTimestampFilter ctf = new ChangeTimestampFilter();
                JsonObject cfg = new JsonObject();
                cfg.addProperty("offset", startTime);
                ctf.init(studio, cfg);
                stream.addFilter(ctf);
            }

            metaDataFilter.setInputReplay(replayFile);
            stream.addFilter(metaDataFilter);

            PacketData packetData;
            while ((packetData = stream.next()) != null) {
                out.write(packetData);
            }
            for (PacketData data : stream.end()) {
                out.write(data);
            }

            startTime += metaData.getDuration();

            updateListener.onProgressChanged((1/4f)+((1/4f)*(i/size)));
        }

        out.close();

        updateListener.onProgressChanged(2/4f, I18n.format("replaymod.gui.editor.progress.status.writing.zip"));

        metaDataFilter.writeTo(outputReplayFile);

        updateListener.onProgressChanged(3/4f, I18n.format("replaymod.gui.editor.progress.status.writing.final"));

        outputReplayFile.saveTo(outputFile);

        updateListener.onProgressChanged(1f, I18n.format("replaymod.gui.editor.progress.status.finished"));
    }
}
