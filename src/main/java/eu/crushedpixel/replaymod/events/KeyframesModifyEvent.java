package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraftforge.fml.common.eventhandler.Event;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class KeyframesModifyEvent extends Event {

    private KeyframeList<AdvancedPosition> positionKeyframes;
    private KeyframeList<TimestampValue> timeKeyframes;

}
