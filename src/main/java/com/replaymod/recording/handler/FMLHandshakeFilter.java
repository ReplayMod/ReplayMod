//#if MC<11400
//$$ package com.replaymod.recording.handler;
//$$
//$$ import io.netty.channel.ChannelHandlerContext;
//$$ import io.netty.channel.SimpleChannelInboundHandler;
//$$ import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
//$$
//$$ /**
//$$  * Filters out all handshake packets that were sent for recording but must
//$$  * not actually be handled.
//$$  * This handler is only present when connected to the integrated server as
//$$  * otherwise all packets must be handled.
//$$  *
//$$  * When in single player, the game state packets must never be handled
//$$  * otherwise wired bugs related to semi-singletons can occur.
//$$  * See https://bugs.replaymod.com/show_bug.cgi?id=44
//$$  */
//$$ public class FMLHandshakeFilter extends SimpleChannelInboundHandler<FMLHandshakeMessage> {
//$$     @Override
//$$     protected void channelRead0(ChannelHandlerContext ctx, FMLHandshakeMessage msg) throws Exception {
        //#if MC>=10800
        //$$ if (!(msg instanceof FMLHandshakeMessage.RegistryData)) {
        //#else
        //$$ if (!(msg instanceof FMLHandshakeMessage.ModIdData)) {
        //#endif
//$$             // Pass on everything but RegistryData messages
//$$             ctx.fireChannelRead(msg);
//$$         }
//$$     }
//$$ }
//#endif
