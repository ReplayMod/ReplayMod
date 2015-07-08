package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraftforge.fml.common.eventhandler.Event;

@Data
@AllArgsConstructor
public class KeyframesModifyEvent extends Event {

    private KeyframeList<Keyframe<Position>> positionKeyframes;
    private KeyframeList<Keyframe<TimestampValue>> timeKeyframes;

}
