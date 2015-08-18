package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    /**
     * Record the own player entity joining the world.
     * We cannot use the {@link net.minecraftforge.event.entity.EntityJoinWorldEvent} because the entity id
     * of the player is set afterwards.
     * @param packet The packet
     * @param ci Callback info
     */
    @Inject(method = "handleJoinGame", at=@At("RETURN"))
    public void recordJoinGame(S01PacketJoinGame packet, CallbackInfo ci) {
        if (ConnectionEventHandler.isRecording()) {
            ReplayMod.recordingHandler.onPlayerJoin();
        }
    }
}
