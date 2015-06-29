package eu.crushedpixel.replaymod.api.replay.holders;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class Donated {

    @Getter(AccessLevel.NONE)
    private boolean donated = false;

    public boolean hasDonated() {
        return donated;
    }
}
