package com.replaymod.recording.mixin;

import net.minecraft.client.network.ClientLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

//#if MC>=11903
//$$ import net.minecraft.client.network.ServerInfo;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//#endif

@Mixin(ClientLoginNetworkHandler.class)
public interface ClientLoginNetworkHandlerAccessor {
    //#if MC>=11903
    //$$ @Accessor
    //$$ ServerInfo getServerInfo();
    //#endif
}
