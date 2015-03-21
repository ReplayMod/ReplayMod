package eu.crushedpixel.replaymod.holders;

public class TimeKeyframe extends Keyframe {

	private int timestamp;
	
	public TimeKeyframe(int realTime, int timestamp) {
		super(realTime);
		this.timestamp = timestamp;
	}

	public int getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
}
