package org.main.redstoneutils.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.main.redstoneutils.RedstoneUtils;

public final class RedstoneUtilsNetworking {

    private RedstoneUtilsNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(SetAutoWirePayload.TYPE, SetAutoWirePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientCommandPayload.TYPE, ClientCommandPayload.CODEC);
    }

    public record SetAutoWirePayload(String mode) implements CustomPacketPayload {
        public static final Type<SetAutoWirePayload> TYPE = new Type<>(RedstoneUtils.id("set_autowire"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetAutoWirePayload> CODEC = StreamCodec.ofMember(
                SetAutoWirePayload::write,
                SetAutoWirePayload::read
        );

        private static SetAutoWirePayload read(RegistryFriendlyByteBuf buf) {
            return new SetAutoWirePayload(buf.readUtf(64));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(mode, 64);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeleportPayload(double range) implements CustomPacketPayload {
        public static final Type<TeleportPayload> TYPE = new Type<>(RedstoneUtils.id("teleport"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPayload> CODEC = StreamCodec.ofMember(
                TeleportPayload::write,
                TeleportPayload::read
        );

        private static TeleportPayload read(RegistryFriendlyByteBuf buf) {
            return new TeleportPayload(buf.readDouble());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeDouble(range);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientCommandPayload(String action, int value) implements CustomPacketPayload {
        public static final Type<ClientCommandPayload> TYPE = new Type<>(RedstoneUtils.id("client_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientCommandPayload> CODEC = StreamCodec.ofMember(
                ClientCommandPayload::write,
                ClientCommandPayload::read
        );

        private static ClientCommandPayload read(RegistryFriendlyByteBuf buf) {
            return new ClientCommandPayload(buf.readUtf(64), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(action, 64);
            buf.writeVarInt(value);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
