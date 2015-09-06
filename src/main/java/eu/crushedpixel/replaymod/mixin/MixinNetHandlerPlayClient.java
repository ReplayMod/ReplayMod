package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards and the tablist entry might not yet be sent.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handlePlayerListItem", at=@At("RETURN"))
    public void recordOwnJoin(S38PacketPlayerListItem packet, CallbackInfo ci) {
        if (ConnectionEventHandler.isRecording() && packet.func_179768_b() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
            @SuppressWarnings("unchecked")
            List<S38PacketPlayerListItem.AddPlayerData> dataList = packet.func_179767_a();
            for (S38PacketPlayerListItem.AddPlayerData data : dataList) {
                if (data.func_179962_a().getId().equals(Minecraft.getMinecraft().thePlayer.getGameProfile().getId())) {
                    ReplayMod.recordingHandler.onPlayerJoin();
                }
            }
        }
    }

    /**
     * Record the own player entity respawning.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because that would also include
     * the first spawn which is already handled by {@link #recordOwnJoin(S38PacketPlayerListItem, CallbackInfo)}.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(S07PacketRespawn packet, CallbackInfo ci) {
        if (ConnectionEventHandler.isRecording()) {
            ReplayMod.recordingHandler.onPlayerRespawn();
        }
    }
}
