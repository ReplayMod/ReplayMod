package eu.crushedpixel.replaymod.events.handlers.keyboard;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class StaticKeybinding {

    private final int id;
    private final int keyCode;
    private boolean down;
}
