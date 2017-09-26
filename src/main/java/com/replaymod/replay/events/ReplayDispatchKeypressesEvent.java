package com.replaymod.replay.events;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public abstract class ReplayDispatchKeypressesEvent extends Event {
    public static class Pre extends ReplayDispatchKeypressesEvent {}
}
