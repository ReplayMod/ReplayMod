package com.replaymod.replay.events;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class ReplayDispatchKeypressesEvent extends Event {

    @Cancelable
    public static class Pre extends ReplayDispatchKeypressesEvent {}
}
