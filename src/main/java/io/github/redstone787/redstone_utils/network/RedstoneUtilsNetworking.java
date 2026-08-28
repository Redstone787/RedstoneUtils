/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.github.redstone787.redstone_utils.RedstoneUtils;

public final class RedstoneUtilsNetworking {

    private RedstoneUtilsNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(SetAutoWirePayload.TYPE, SetAutoWirePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BackendProbePayload.TYPE, BackendProbePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientCommandPayload.TYPE, ClientCommandPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AutoWireFeedbackPayload.TYPE, AutoWireFeedbackPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AutoWireStatePayload.TYPE, AutoWireStatePayload.CODEC);
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

    public record BackendProbePayload() implements CustomPacketPayload {
        public static final BackendProbePayload INSTANCE = new BackendProbePayload();
        public static final Type<BackendProbePayload> TYPE = new Type<>(RedstoneUtils.id("backend_probe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BackendProbePayload> CODEC = StreamCodec.unit(INSTANCE);

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

    public record AutoWireFeedbackPayload(String reason, String componentTranslationKey) implements CustomPacketPayload {
        public static final Type<AutoWireFeedbackPayload> TYPE = new Type<>(RedstoneUtils.id("autowire_feedback"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AutoWireFeedbackPayload> CODEC = StreamCodec.ofMember(
                AutoWireFeedbackPayload::write,
                AutoWireFeedbackPayload::read
        );

        private static AutoWireFeedbackPayload read(RegistryFriendlyByteBuf buf) {
            return new AutoWireFeedbackPayload(buf.readUtf(64), buf.readUtf(128));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(reason, 64);
            buf.writeUtf(componentTranslationKey, 128);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AutoWireStatePayload(String mode, boolean accepted) implements CustomPacketPayload {
        public static final Type<AutoWireStatePayload> TYPE = new Type<>(RedstoneUtils.id("autowire_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AutoWireStatePayload> CODEC = StreamCodec.ofMember(
                AutoWireStatePayload::write,
                AutoWireStatePayload::read
        );

        private static AutoWireStatePayload read(RegistryFriendlyByteBuf buf) {
            return new AutoWireStatePayload(buf.readUtf(64), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(mode, 64);
            buf.writeBoolean(accepted);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
