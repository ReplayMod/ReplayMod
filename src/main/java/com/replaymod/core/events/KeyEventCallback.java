package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (key, scanCode, action, modifiers) -> {
                for (KeyEventCallback listener : listeners) {
                    if (listener.onKeyEvent(key, scanCode, action, modifiers)) {
                        return true;
                    }
                }
                return false;
            }
    );

    //#if MC>=11400
    int ACTION_RELEASE = org.lwjgl.glfw.GLFW.GLFW_RELEASE;
    int ACTION_PRESS = org.lwjgl.glfw.GLFW.GLFW_PRESS;
    //#else
    //$$ int ACTION_RELEASE = 0;
    //$$ int ACTION_PRESS = 1;
    //#endif

    boolean onKeyEvent(int key, int scanCode, int action, int modifiers);
}
