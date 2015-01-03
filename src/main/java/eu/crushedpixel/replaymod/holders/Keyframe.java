package eu.crushedpixel.replaymod.holders;

public class Keyframe {

	private int realTimestamp;
	
	public Keyframe(int realTimestamp) {
		this.realTimestamp = realTimestamp;
	}

	public int getRealTimestamp() {
		return realTimestamp;
	}

	public void setRealTimestamp(int realTimestamp) {
		this.realTimestamp = realTimestamp;
	}
	
	
}
