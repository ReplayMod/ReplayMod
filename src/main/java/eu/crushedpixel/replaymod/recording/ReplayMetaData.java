package eu.crushedpixel.replaymod.recording;

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

    public ReplayMetaData(boolean singleplayer, String serverName, String generator,
                          int duration, long date, String[] players, String mcversion) {
        this.singleplayer = singleplayer;
        this.serverName = serverName;
        this.generator = generator;
        this.duration = duration;
        this.date = date;
        this.players = players;
        this.mcversion = mcversion;
    }

    public void removeServer() { this.serverName = null; }

    public boolean isSingleplayer() {
        return singleplayer;
    }

    public String getServerName() {
        return serverName;
    }

    public String getGenerator() {
        return generator;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getDate() {
        return date;
    }

    public String[] getPlayers() {
        return players;
    }

    public String getMCVersion() {
        return mcversion;
    }
}
