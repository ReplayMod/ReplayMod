package com.replaymod.recording.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketEnabledPacksData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientConfigurationPacketListener;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.config.ReadyS2CPacket;
import net.minecraft.network.state.ConfigurationStates;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientConfigurationNetworkHandler.class)
public abstract class MixinNetHandlerConfigClient {
    @Inject(method = "onReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;transitionInbound(Lnet/minecraft/network/NetworkState;Lnet/minecraft/network/listener/PacketListener;)V"))
    public void recordEnabledPackData(CallbackInfo ci, @Local DynamicRegistryManager.Immutable registryManager) {
        PacketListener packetListener = ReplayModRecording.instance.getConnectionEventHandler().getPacketListener();
        if (packetListener == null) return;

        ByteBuf byteBuf = Unpooled.buffer();
        PacketByteBuf buf = new PacketByteBuf(byteBuf);
        buf.writeString(PacketEnabledPacksData.ID);
        buf.writeVarInt(1);
        write(buf, registryManager.get(RegistryKeys.DIMENSION_TYPE), DimensionType.CODEC);

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        byteBuf.release();

        int packetIdCustomPayload = getPacketId(new CustomPayloadS2CPacket(new BrandCustomPayload("")));
        int packetIdFinish = getPacketId(ReadyS2CPacket.INSTANCE);

        PacketTypeRegistry registry = MCVer.getPacketTypeRegistry(State.CONFIGURATION);
        packetListener.save(new Packet(registry, packetIdCustomPayload, PacketType.ConfigCustomPayload, com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(bytes)));
        packetListener.save(new Packet(registry, packetIdFinish, PacketType.ConfigFinish, com.github.steveice10.netty.buffer.Unpooled.buffer()));
    }

    @Unique
    private <T> void write(PacketByteBuf buf, Registry<T> registry, Codec<T> codec) {
        buf.writeString(registry.getKey().getValue().toString());
        buf.writeVarInt(registry.size());
        for (Map.Entry<RegistryKey<T>, T> entry : registry.getEntrySet()) {
            buf.writeString(entry.getKey().getValue().toString());
            buf.writeNbt(codec.encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow());
        }
    }

    @Unique
    private int getPacketId(net.minecraft.network.packet.Packet<? super ClientConfigurationPacketListener> packet) {
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            //#if MC>=12106
            //$$ ConfigurationStates.S2C.comp_2236().encode(byteBuf, packet);
            //#else
            ConfigurationStates.S2C.codec().encode(byteBuf, packet);
            //#endif
            return new PacketByteBuf(byteBuf).readVarInt();
        } finally {
            byteBuf.release();
        }
    }
}
