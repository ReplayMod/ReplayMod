package eu.crushedpixel.replaymod.holders;

import java.util.List;

public class PlayerVisibility {

    public PlayerVisibility(List<Integer> hidden) {
        this.hidden = hidden.toArray(new Integer[hidden.size()]);
    }

    private Integer[] hidden;

    public Integer[] getHiddenPlayers() {
        return hidden;
    }

}
