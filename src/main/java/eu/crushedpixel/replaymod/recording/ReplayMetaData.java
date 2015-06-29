package eu.crushedpixel.replaymod.recording;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReplayMetaData {

    private boolean singleplayer;
    private String serverName;
    private String generator;
    private int duration;
    private long date;
    private String[] players;
    private String mcversion;

    public ReplayMetaData copy() {
        return new ReplayMetaData(this.singleplayer, this.serverName, this.generator,
                this.duration, this.date, this.players, this.mcversion);
    }

    public void removeServer() { this.serverName = null; }
}
