package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.replay.ReplayProcess;
import net.minecraft.util.Timer;

public class ReplayTimer extends Timer {

	public ReplayTimer(float p_i1018_1_) {
		super(p_i1018_1_);
	}

	@Override
	public void updateTimer() {
		if(ReplayProcess.isVideoRecording()) return;
		super.updateTimer();
	}
}
