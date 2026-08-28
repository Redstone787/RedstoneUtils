/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstone_utils.network.RedstoneUtilsNetworking;
import io.github.redstone787.redstone_utils.server.autowire.ServerAutoWire;
import io.github.redstone787.redstone_utils.server.autowire.WireType;

public final class RedstoneUtilsServerNetworking {

    private RedstoneUtilsServerNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.SetAutoWirePayload.TYPE, (payload, context) -> {
            if (!RedstoneUtilsCommands.canUseAutoWire(context.player().createCommandSourceStack())) {
                ServerAutoWire.deny(context.player());
                context.player().sendSystemMessage(Component.translatable("message.redstone_utils.permission.autowire"));
                return;
            }

            WireType wireType = WireType.find(payload.mode()).orElse(WireType.NONE);
            ServerAutoWire.setWireType(context.player(), wireType);
        });

        ServerPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.BackendProbePayload.TYPE, (payload, context) -> {
            // Registration of this payload is the capability handshake; no response is required.
        });

        ServerPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.TeleportPayload.TYPE, (payload, context) -> {
            if (!RedstoneUtilsCommands.canUseTeleport(context.player().createCommandSourceStack())) {
                context.player().sendSystemMessage(Component.translatable("message.redstone_utils.permission.teleport"));
                return;
            }

            RedstoneUtilsCommands.teleportPlayer(context.player(), payload.range());
        });
    }
}
