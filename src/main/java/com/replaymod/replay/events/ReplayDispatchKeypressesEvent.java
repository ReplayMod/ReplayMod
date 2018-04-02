package com.replaymod.replay.events;

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import cpw.mods.fml.common.eventhandler.Cancelable;
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif

public abstract class ReplayDispatchKeypressesEvent extends Event {

    @Cancelable
    public static class Pre extends ReplayDispatchKeypressesEvent {}
}
