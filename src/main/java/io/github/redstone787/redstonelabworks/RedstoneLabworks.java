/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import io.github.redstone787.redstonelabworks.network.RedstoneLabworksNetworking;
import io.github.redstone787.redstonelabworks.server.RedstoneLabworksCommands;
import io.github.redstone787.redstonelabworks.server.RedstoneLabworksServerNetworking;
import io.github.redstone787.redstonelabworks.server.autowire.ServerAutoWire;
import io.github.redstone787.redstonelabworks.server.config.RedstoneLabworksServerConfig;
import io.github.redstone787.redstonelabworks.server.gamerule.RedstoneLabworksGameRules;
import io.github.redstone787.redstonelabworks.server.history.ChangeHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneLabworks implements ModInitializer {
    public static final String MOD_ID = "redstonelabworks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RedstoneLabworksGameRules.init();
        RedstoneLabworksServerConfig.load();
        RedstoneLabworksNetworking.init();
        ChangeHistory.init();
        ServerAutoWire.init();
        RedstoneLabworksCommands.init();
        RedstoneLabworksServerNetworking.init();
    }
}
