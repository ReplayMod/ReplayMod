package eu.crushedpixel.replaymod.holders;

public class PositionKeyframe extends Keyframe {

	private Position position;
	
	public PositionKeyframe(int realTime, Position position) {
		super(realTime);
		this.position = position;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}
	
}