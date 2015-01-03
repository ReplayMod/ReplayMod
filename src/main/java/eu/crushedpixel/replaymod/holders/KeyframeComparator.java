package eu.crushedpixel.replaymod.holders;

import java.util.Comparator;

import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class KeyframeComparator implements Comparator<Keyframe> {

	@Override
	public int compare(Keyframe o1, Keyframe o2) {
		if(ReplayHandler.isSelected(o1)) return 1;
		if(ReplayHandler.isSelected(o2)) return -1;
		return o1.getRealTimestamp()-o2.getRealTimestamp();
	}

}
