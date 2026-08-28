/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstonelabworks.network.RedstoneLabworksNetworking;
import io.github.redstone787.redstonelabworks.server.autowire.ServerAutoWire;
import io.github.redstone787.redstonelabworks.server.autowire.WireType;

public final class RedstoneLabworksServerNetworking {

    private RedstoneLabworksServerNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(RedstoneLabworksNetworking.SetAutoWirePayload.TYPE, (payload, context) -> {
            if (!RedstoneLabworksCommands.canUseAutoWire(context.player().createCommandSourceStack())) {
                ServerAutoWire.deny(context.player());
                context.player().sendSystemMessage(Component.translatable("message.redstonelabworks.permission.autowire"));
                return;
            }

            WireType wireType = WireType.find(payload.mode()).orElse(WireType.NONE);
            ServerAutoWire.setWireType(context.player(), wireType);
        });

        ServerPlayNetworking.registerGlobalReceiver(RedstoneLabworksNetworking.BackendProbePayload.TYPE, (payload, context) -> {
            // Registration of this payload is the capability handshake; no response is required.
        });

        ServerPlayNetworking.registerGlobalReceiver(RedstoneLabworksNetworking.TeleportPayload.TYPE, (payload, context) -> {
            if (!RedstoneLabworksCommands.canUseTeleport(context.player().createCommandSourceStack())) {
                context.player().sendSystemMessage(Component.translatable("message.redstonelabworks.permission.teleport"));
                return;
            }

            RedstoneLabworksCommands.teleportPlayer(context.player(), payload.range());
        });
    }
}
