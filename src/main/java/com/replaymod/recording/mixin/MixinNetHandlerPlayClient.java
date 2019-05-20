package com.replaymod.recording.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10800
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.packetlib.io.buffer.ByteBufferNetInput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.client.network.packet.PlayerListS2CPacket;
import net.minecraft.client.network.PlayerListEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
//#endif

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetHandlerPlayClient {

    // The stupid name is required as otherwise Mixin treats it as a shadow, seemingly ignoring the lack of @Shadow
    private static MinecraftClient mcStatic = MCVer.getMinecraft();

    //#if MC>=10800
    @Shadow
    private Map<UUID, PlayerListEntry> playerListEntries;
    //#endif

    public RecordingEventHandler getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) mcStatic.worldRenderer).getRecordingEventHandler();
    }

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards and the tablist entry might not yet be sent.
     * @param packet The packet
     * @param ci Callback info
     */
    //#if MC>=10800
    @Inject(method = "onPlayerList", at=@At("HEAD"))
    public void recordOwnJoin(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (mcStatic.player == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null && packet.getAction() == PlayerListS2CPacket.Action.ADD_PLAYER) {
            // We cannot reference SPacketPlayerListItem.AddPlayerData directly for complicated (and yet to be
            // resolved) reasons (see https://github.com/MinecraftForge/ForgeGradle/issues/472), so we "simply" convert
            // the back to the MCProtocolLib equivalent and deal with that one.
            ByteBuf byteBuf = Unpooled.buffer();
            ServerPlayerListEntryPacket mcpl = new ServerPlayerListEntryPacket(null, null);
            try {
                packet.write(new PacketByteBuf(byteBuf));

                byteBuf.readerIndex(0);
                byte[] array = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(array);

                mcpl.read(new ByteBufferNetInput(ByteBuffer.wrap(array)));
            } catch (IOException e) {
                throw new RuntimeException(e); // we just parsed this?
            } finally {
                byteBuf.release();
            }

            for (com.github.steveice10.mc.protocol.data.game.PlayerListEntry data : mcpl.getEntries()) {
                if (data.getProfile() == null || data.getProfile().getId() == null) continue;
                // Only add spawn packet for our own player and only if he isn't known yet
                if (data.getProfile().getId().equals(mcStatic.player.getGameProfile().getId())
                        && !this.playerListEntries.containsKey(data.getProfile().getId())) {
                    handler.onPlayerJoin();
                }
            }
        }
    }
    //#else
    //$$ @Inject(method = "handleJoinGame", at=@At("RETURN"))
    //$$ public void recordOwnJoin(S01PacketJoinGame packet, CallbackInfo ci) {
    //$$     RecordingEventHandler handler = getRecordingEventHandler();
    //$$     if (handler != null) {
    //$$         handler.onPlayerJoin();
    //$$     }
    //$$ }
    //#endif

    /**
     * Record the own player entity respawning.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because that would also include
     * the first spawn which is already handled by the above method.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "onPlayerRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerRespawn();
        }
    }
}
