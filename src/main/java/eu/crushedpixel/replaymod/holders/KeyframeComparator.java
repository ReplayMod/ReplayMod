package eu.crushedpixel.replaymod.holders;

import java.util.Comparator;

public class KeyframeComparator implements Comparator<Keyframe> {

    @Override
    public int compare(Keyframe o1, Keyframe o2) {
        if(o1 == null || o2 == null) return 0;
        return ((Integer) o1.getRealTimestamp()).compareTo(o2.getRealTimestamp());
    }

}
