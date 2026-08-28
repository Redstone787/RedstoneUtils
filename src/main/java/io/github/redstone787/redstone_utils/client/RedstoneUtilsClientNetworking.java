/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import io.github.redstone787.redstone_utils.client.autowire.WireType;
import io.github.redstone787.redstone_utils.client.autowire.AutoWireHandler;
import io.github.redstone787.redstone_utils.client.ui.RedstoneMessages;
import io.github.redstone787.redstone_utils.network.RedstoneUtilsNetworking;

import java.util.Locale;

public final class RedstoneUtilsClientNetworking {

    private RedstoneUtilsClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.ClientCommandPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RedstoneUtilsClientActions.run(payload.action(), payload.value()))
        );
        ClientPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.AutoWireFeedbackPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RedstoneMessages.popup(Component.translatable(
                        "message.redstone_utils.autowire.failure." + payload.reason(),
                        Component.translatable(payload.componentTranslationKey())
                )))
        );
        ClientPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.AutoWireStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> AutoWireHandler.applyServerMode(payload.mode(), payload.accepted()))
        );
    }

    public static boolean hasServerBackend() {
        return canSend(RedstoneUtilsNetworking.BackendProbePayload.TYPE);
    }

    public static boolean hasAutoWireBackend() {
        return canSend(RedstoneUtilsNetworking.SetAutoWirePayload.TYPE);
    }

    public static boolean hasTeleportBackend() {
        return canSend(RedstoneUtilsNetworking.TeleportPayload.TYPE);
    }

    private static boolean canSend(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        try {
            return ClientPlayNetworking.canSend(type);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean setServerAutoWire(WireType wireType) {
        if (!hasAutoWireBackend()) return false;

        String mode = wireType == null
                ? "none"
                : wireType.name().toLowerCase(Locale.ROOT);
        ClientPlayNetworking.send(new RedstoneUtilsNetworking.SetAutoWirePayload(mode));
        return true;
    }

    public static boolean teleport(double range) {
        if (!hasTeleportBackend()) return false;

        ClientPlayNetworking.send(new RedstoneUtilsNetworking.TeleportPayload(range));
        return true;
    }

    /**
     * Sends a command straight to the server without passing it through Fabric's
     * client-command dispatcher a second time.
     */
    public static boolean sendServerCommand(String command) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || command == null) return false;

        String normalized = command.strip();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank()) return false;

        connection.getConnection().send(new ServerboundChatCommandPacket(normalized));
        return true;
    }
}
