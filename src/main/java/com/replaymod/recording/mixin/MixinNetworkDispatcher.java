//#if MC<11300
//$$ package com.replaymod.recording.mixin;
//$$
//$$ import com.replaymod.recording.handler.FMLHandshakeFilter;
//$$ import io.netty.channel.ChannelPipeline;
//$$ import io.netty.channel.embedded.EmbeddedChannel;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//#if MC>=11200
//$$ import io.netty.channel.ChannelConfig;
//$$ import net.minecraft.network.EnumConnectionState;
//$$ import net.minecraft.network.NetworkManager;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//#endif
//$$
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.network.handshake.FMLHandshakeCodec;
//$$ import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
//$$ import net.minecraftforge.fml.relauncher.Side;
//#else
//$$ import cpw.mods.fml.common.network.handshake.FMLHandshakeCodec;
//$$ import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
//$$ import cpw.mods.fml.relauncher.Side;
//#endif
//$$
//$$ @Mixin(value = NetworkDispatcher.class, remap = false)
//$$ public abstract class MixinNetworkDispatcher {
//$$
//$$     @Shadow
//$$     private Side side;
//$$
//$$     @Shadow
//$$     private EmbeddedChannel handshakeChannel;
//$$
//$$     /**
//$$      * Always sets fml:isLocal to false on the server side.
//$$      * This effectively removes the difference in the FML handshake between SP and MP
//$$      * and forces the block/item ids, etc. to always be send.
//$$      * Injects a {@link FMLHandshakeFilter} on the client side to filter out
//$$      * those extra, unexpected packets.
//$$      */
//$$     @Inject(method = "insertIntoChannel", at=@At("HEAD"))
//$$     public void replayModRecording_setupForLocalRecording(CallbackInfo cb) {
//$$         // If we're in multiplayer, everything is fine as is
//$$         if (!handshakeChannel.attr(NetworkDispatcher.IS_LOCAL).get()) return;
//$$
//$$         if (side == Side.SERVER) {
//$$             // On the server side, force all packets to be sent
//$$             handshakeChannel.attr(NetworkDispatcher.IS_LOCAL).set(false);
//$$         } else {
//$$             // On the client side, discard additional packets
//$$             ChannelPipeline pipeline = handshakeChannel.pipeline();
//$$             pipeline.addAfter(pipeline.context(FMLHandshakeCodec.class).name(),
//$$                     "replaymod_filter", new FMLHandshakeFilter());
//$$         }
//$$     }
//$$
    //#if MC>=11200
    //$$ @Redirect(method = "clientListenForServerHandshake", at = @At(value = "INVOKE", remap = true, target =
    //$$         "Lnet/minecraft/network/NetworkManager;setConnectionState(Lnet/minecraft/network/EnumConnectionState;)V"))
    //$$ public void replayModRecording_raceConditionWorkAround1(NetworkManager self, EnumConnectionState ignored) { }
    //$$
    //$$ @Redirect(method = "insertIntoChannel", at = @At(value = "INVOKE", target =
    //$$         "Lio/netty/channel/ChannelConfig;setAutoRead(Z)Lio/netty/channel/ChannelConfig;"))
    //$$ public ChannelConfig replayModRecording_raceConditionWorkAround2(ChannelConfig self, boolean autoRead) {
    //$$     if (side == Side.CLIENT) {
    //$$         autoRead = false;
    //$$     }
    //$$     return self.setAutoRead(autoRead);
    //$$ }
    //#endif
//$$ }
//#endif
