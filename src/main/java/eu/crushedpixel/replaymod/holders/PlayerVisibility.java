package eu.crushedpixel.replaymod.holders;

import java.util.Collection;
import java.util.UUID;

public class PlayerVisibility {

    public PlayerVisibility(Collection<UUID> hidden) {
        this.hidden = hidden.toArray(new UUID[hidden.size()]);
    }

    private UUID[] hidden;

    public UUID[] getHiddenPlayers() {
        return hidden;
    }

}
