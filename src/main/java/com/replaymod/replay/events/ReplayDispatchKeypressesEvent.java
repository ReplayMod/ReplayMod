package com.replaymod.replay.events;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.Cancelable;
//$$ import net.minecraftforge.fml.common.eventhandler.Event;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.Cancelable;
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif

public abstract class ReplayDispatchKeypressesEvent extends Event {

    @Cancelable
    public static class Pre extends ReplayDispatchKeypressesEvent {}
}
