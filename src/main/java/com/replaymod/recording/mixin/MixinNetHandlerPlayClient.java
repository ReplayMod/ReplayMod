package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketRespawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;

    @Shadow
    private Map<UUID, NetworkPlayerInfo> playerInfoMap;

    public RecordingEventHandler getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) gameController.renderGlobal).getRecordingEventHandler();
    }

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards and the tablist entry might not yet be sent.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handlePlayerListItem", at=@At("HEAD"))
    public void recordOwnJoin(SPacketPlayerListItem packet, CallbackInfo ci) {
        if (gameController.player == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null && packet.getAction() == SPacketPlayerListItem.Action.ADD_PLAYER) {
            for (SPacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
                if (data.getProfile() == null || data.getProfile().getId() == null) continue;
                // Only add spawn packet for our own player and only if he isn't known yet
                if (data.getProfile().getId().equals(Minecraft.getMinecraft().player.getGameProfile().getId())
                        && !playerInfoMap.containsKey(data.getProfile().getId())) {
                    handler.onPlayerJoin();
                }
            }
        }
    }

    /**
     * Record the own player entity respawning.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because that would also include
     * the first spawn which is already handled by {@link #recordOwnJoin(SPacketPlayerListItem, CallbackInfo)}.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(SPacketRespawn packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerRespawn();
        }
    }
}
