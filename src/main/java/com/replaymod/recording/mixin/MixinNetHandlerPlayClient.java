package com.replaymod.recording.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10800
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
//#endif

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    private static Minecraft gameController = MCVer.getMinecraft();

    //#if MC>=10800
    @Shadow
    private Map<UUID, NetworkPlayerInfo> playerInfoMap;
    //#endif

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
    //#if MC>=10800
    @Inject(method = "handlePlayerListItem", at=@At("HEAD"))
    public void recordOwnJoin(SPacketPlayerListItem packet, CallbackInfo ci) {
        if (gameController.player == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null && packet.getAction() == SPacketPlayerListItem.Action.ADD_PLAYER) {
            //#if MC>=10809
            List<SPacketPlayerListItem.AddPlayerData> entries = packet.getEntries();
            //#else
            //$$ @SuppressWarnings("unchecked")
            //$$ List<S38PacketPlayerListItem.AddPlayerData> entries = packet.func_179767_a();
            //#endif
            for (SPacketPlayerListItem.AddPlayerData data : entries) {
                if (data.getProfile() == null || data.getProfile().getId() == null) continue;
                // Only add spawn packet for our own player and only if he isn't known yet
                if (data.getProfile().getId().equals(gameController.player.getGameProfile().getId())
                        && !this.playerInfoMap.containsKey(data.getProfile().getId())) {
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
    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(SPacketRespawn packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerRespawn();
        }
    }
}
