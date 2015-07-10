package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuiEntryListValueEntry<T> implements GuiEntryListEntry {

    String displayString;
    T value;

    @Override
    public String getDisplayString() {
        return displayString;
    }
}
