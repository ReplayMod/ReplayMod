package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.replay.ReplayHandler;

import java.util.Comparator;

public class KeyframeComparator implements Comparator<Keyframe> {

    @Override
    public int compare(Keyframe o1, Keyframe o2) {
        if(ReplayHandler.isSelected(o1)) return 1;
        if(ReplayHandler.isSelected(o2)) return -1;
        return ((Integer) o1.getRealTimestamp()).compareTo(o2.getRealTimestamp());
    }

}
