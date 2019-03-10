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
//#if MC>=10904
import net.minecraft.network.play.server.SPacketPlayerListItem.Action;
import net.minecraft.network.play.server.SPacketPlayerListItem.AddPlayerData;
//#else
//$$ import net.minecraft.network.play.server.S38PacketPlayerListItem.Action;
//$$ import net.minecraft.network.play.server.S38PacketPlayerListItem.AddPlayerData;
//#endif
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.replaymod.core.versions.MCVer.*;
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
    //#if MC>=10809
    //#if MC>=10904
    public void recordOwnJoin(SPacketPlayerListItem packet, CallbackInfo ci) {
    //#else
    //$$ public void recordOwnJoin(S38PacketPlayerListItem packet, CallbackInfo ci) {
    //#endif
        if (player(gameController) == null) return;

        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null && packet.getAction() == Action.ADD_PLAYER) {
            for (AddPlayerData data : packet.getEntries()) {
                if (data.getProfile() == null || data.getProfile().getId() == null) continue;
                // Only add spawn packet for our own player and only if he isn't known yet
                if (data.getProfile().getId().equals(player(gameController).getGameProfile().getId())
                        && !playerInfoMap.containsKey(data.getProfile().getId())) {
                    handler.onPlayerJoin();
                }
            }
        }
    }
    //#else
    //$$ public void recordOwnJoin(S38PacketPlayerListItem packet, CallbackInfo ci) {
    //$$     if (gameController.thePlayer == null) return;
    //$$
    //$$     RecordingEventHandler handler = getRecordingEventHandler();
    //$$     if (handler != null && packet.func_179768_b() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
    //$$         @SuppressWarnings("unchecked")
    //$$         List<S38PacketPlayerListItem.AddPlayerData> dataList = packet.func_179767_a();
    //$$         for (S38PacketPlayerListItem.AddPlayerData data : dataList) {
    //$$             if (data.func_179962_a() == null || data.func_179962_a().getId() == null) continue;
    //$$             // Only add spawn packet for our own player and only if he isn't known yet
    //$$             if (data.func_179962_a().getId().equals(Minecraft.getMinecraft().thePlayer.getGameProfile().getId())
    //$$                     && !playerInfoMap.containsKey(data.func_179962_a().getId())) {
    //$$                 handler.onPlayerJoin();
    //$$             }
    //$$         }
    //$$     }
    //$$ }
    //#endif
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
    //#if MC>=10904
    public void recordOwnRespawn(SPacketRespawn packet, CallbackInfo ci) {
    //#else
    //$$ public void recordOwnRespawn(S07PacketRespawn packet, CallbackInfo ci) {
    //#endif
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerRespawn();
        }
    }
}
