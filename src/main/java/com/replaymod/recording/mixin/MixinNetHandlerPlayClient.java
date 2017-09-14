package com.replaymod.recording.mixin;

import com.replaymod.recording.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;

    public RecordingEventHandler getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) gameController.renderGlobal).getRecordingEventHandler();
    }

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handleJoinGame", at=@At("RETURN"))
    public void recordOwnJoin(S01PacketJoinGame packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerJoin();
        }
    }

    /**
     * Record the own player entity respawning.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because that would also include
     * the first spawn which is already handled by {@link #recordOwnJoin(S01PacketJoinGame, CallbackInfo)}.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(S07PacketRespawn packet, CallbackInfo ci) {
        RecordingEventHandler handler = getRecordingEventHandler();
        if (handler != null) {
            handler.onPlayerRespawn();
        }
    }
}
