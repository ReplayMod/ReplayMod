package com.replaymod.recording.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10800
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Map;
import java.util.UUID;
//#else
//$$ import net.minecraft.network.play.server.S01PacketJoinGame;
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
    //#if FABRIC>=1
    @Inject(method = "onPlayerList", at=@At("HEAD"))
    //#else
    //$$ @Inject(method = "handlePlayerListItem", at=@At("HEAD"))
    //#endif
    public void recordOwnJoin(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (!mcStatic.isOnThread()) return;
        if (mcStatic.player == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        //#if MC>=11903
        //$$ if (handler != null && packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
        //#else
        if (handler != null && packet.getAction() == PlayerListS2CPacket.Action.ADD_PLAYER) {
        //#endif
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                UUID uuid = entry.getProfile().getId();
                // Only add spawn packet for our own player and only if he isn't known yet
                if (uuid.equals(mcStatic.player.getGameProfile().getId()) && !this.playerListEntries.containsKey(uuid)) {
                    handler.spawnRecordingPlayer();
                }
            }
        }
    }
    //#else
    //$$ @Inject(method = "handleJoinGame", at=@At("RETURN"))
    //$$ public void recordOwnJoin(S01PacketJoinGame packet, CallbackInfo ci) {
    //$$     RecordingEventHandler handler = getRecordingEventHandler();
    //$$     if (handler != null) {
    //$$         handler.spawnRecordingPlayer();
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
    //#if FABRIC>=1
    @Inject(method = "onPlayerRespawn", at=@At("RETURN"))
    //#else
    //$$ @Inject(method = "handleRespawn", at=@At("RETURN"))
    //#endif
    public void recordOwnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.spawnRecordingPlayer();
        }
    }
}
