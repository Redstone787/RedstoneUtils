/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.config;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWireHandler;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWirePreviewOverlay;
import io.github.redstone787.redstonelabworks.client.bud.BudSwitchOverlay;
import io.github.redstone787.redstonelabworks.client.macro.MacroStore;
import io.github.redstone787.redstonelabworks.client.sculk.SculkSensorOverlay;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneOverlay;

import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.world.level.storage.LevelResource;

/** Selects a persistent world/server profile whenever the play connection changes. */
public final class ClientProfiles {

    private ClientProfiles() {
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> client.execute(() -> {
            activate(profileKey(client, listener.getConnection().getRemoteAddress()));
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(() ->
                activate(RedstoneLabworksConfig.GLOBAL_PROFILE)
        ));
    }

    private static void activate(String key) {
        RedstoneLabworksConfig.activateProfile(key);
        MacroStore.activateProfile(key);
        RedstoneOverlay.setVisible(RedstoneLabworksConfig.isHudOverlayVisible());
        AutoWirePreviewOverlay.setVisible(RedstoneLabworksConfig.isWirePreviewOverlayVisible());
        BudSwitchOverlay.setVisible(RedstoneLabworksConfig.isBudOverlayVisible());
        SculkSensorOverlay.setVisible(RedstoneLabworksConfig.isSculkOverlayVisible());
        AutoWireHandler.reloadFromProfile();
    }

    private static String profileKey(Minecraft client, SocketAddress remoteAddress) {
        if (client.getSingleplayerServer() != null) {
            java.nio.file.Path worldPath = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize();
            java.nio.file.Path directory = worldPath.getFileName();
            String worldId = directory == null ? worldPath.toString() : directory.toString();
            return "world:" + normalize(worldId);
        }
        ServerData serverData = client.getCurrentServer();
        String address = serverData == null ? String.valueOf(remoteAddress) : serverData.ip;
        return "server:" + normalize(address);
    }

    private static String normalize(String value) {
        String normalized = value == null ? "unknown" : value.strip().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "unknown" : normalized;
    }
}
