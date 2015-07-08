package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarkerKeyframe {

    private int realTimestamp;
    private Position position;
    private String name;

}
