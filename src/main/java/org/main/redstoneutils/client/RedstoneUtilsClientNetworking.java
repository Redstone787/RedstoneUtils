package org.main.redstoneutils.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.main.redstoneutils.client.autowire.WireType;
import org.main.redstoneutils.network.RedstoneUtilsNetworking;

import java.util.Locale;

public final class RedstoneUtilsClientNetworking {

    private RedstoneUtilsClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(RedstoneUtilsNetworking.ClientCommandPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RedstoneUtilsClientActions.run(payload.action(), payload.value()))
        );
    }

    public static boolean hasServerBackend() {
        try {
            return ClientPlayNetworking.canSend(RedstoneUtilsNetworking.SetAutoWirePayload.TYPE)
                    && ClientPlayNetworking.canSend(RedstoneUtilsNetworking.TeleportPayload.TYPE);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean setServerAutoWire(WireType wireType) {
        if (!hasServerBackend()) return false;

        String mode = wireType == null
                ? "none"
                : wireType.name().toLowerCase(Locale.ROOT);
        ClientPlayNetworking.send(new RedstoneUtilsNetworking.SetAutoWirePayload(mode));
        return true;
    }

    public static boolean teleport(double range) {
        if (!hasServerBackend()) return false;

        ClientPlayNetworking.send(new RedstoneUtilsNetworking.TeleportPayload(range));
        return true;
    }
}
