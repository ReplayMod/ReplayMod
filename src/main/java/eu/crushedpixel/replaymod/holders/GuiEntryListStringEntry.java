package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GuiEntryListStringEntry implements GuiEntryListEntry {

    private String displayName;

    @Override
    public String getDisplayString() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
