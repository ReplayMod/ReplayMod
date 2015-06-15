package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.holders.Keyframe;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

public class KeyframesModifyEvent extends Event {

    public List<Keyframe> keyframes;

    public KeyframesModifyEvent(List<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }
}
