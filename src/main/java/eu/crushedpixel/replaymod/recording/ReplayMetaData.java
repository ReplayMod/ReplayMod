package eu.crushedpixel.replaymod.recording;

import java.util.Date;
import java.util.List;

public class ReplayMetaData {
	
	private boolean singleplayer;
	private String serverName;
	private int duration;
	private long date;
	
	
	public ReplayMetaData(boolean singleplayer, String serverName,
			int duration, long date) {
		this.singleplayer = singleplayer;
		this.serverName = serverName;
		this.duration = duration;
		this.date = date;
	}
	public boolean isSingleplayer() {
		return singleplayer;
	}
	public void setSingleplayer(boolean singleplayer) {
		this.singleplayer = singleplayer;
	}
	public String getServerName() {
		return serverName;
	}
	public void setServer_name(String serverName) {
		this.serverName = serverName;
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
	public void setDate(long date) {
		this.date = date;
	}
}
