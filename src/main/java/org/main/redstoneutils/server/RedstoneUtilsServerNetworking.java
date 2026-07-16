package org.main.redstoneutils.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import org.main.redstoneutils.network.RedstoneUtilsNetworking;
import org.main.redstoneutils.server.autowire.ServerAutoWire;
import org.main.redstoneutils.server.autowire.WireType;

public final class RedstoneUtilsServerNetworking {

    private RedstoneUtilsServerNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.SetAutoWirePayload.TYPE, (payload, context) -> {
            if (!RedstoneUtilsCommands.canUse(context.player().createCommandSourceStack())) {
                return;
            }

            WireType wireType = WireType.find(payload.mode()).orElse(WireType.NONE);
            ServerAutoWire.setWireType(context.player(), wireType);
            context.player().sendSystemMessage(Component.literal("AutoWire: " + wireType.displayName()));
        });

        ServerPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.TeleportPayload.TYPE, (payload, context) -> {
            if (!RedstoneUtilsCommands.canUse(context.player().createCommandSourceStack())) {
                return;
            }

            RedstoneUtilsCommands.teleportPlayer(context.player(), payload.range());
        });
    }
}
