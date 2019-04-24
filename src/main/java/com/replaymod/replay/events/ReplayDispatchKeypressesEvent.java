package com.replaymod.replay.events;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class ReplayDispatchKeypressesEvent extends Event {

    @Cancelable
    public static class Pre extends ReplayDispatchKeypressesEvent {}
}
