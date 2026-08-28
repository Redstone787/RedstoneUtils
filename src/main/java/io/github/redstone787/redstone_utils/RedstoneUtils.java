/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import io.github.redstone787.redstone_utils.network.RedstoneUtilsNetworking;
import io.github.redstone787.redstone_utils.server.RedstoneUtilsCommands;
import io.github.redstone787.redstone_utils.server.RedstoneUtilsServerNetworking;
import io.github.redstone787.redstone_utils.server.autowire.ServerAutoWire;
import io.github.redstone787.redstone_utils.server.config.RedstoneUtilsServerConfig;
import io.github.redstone787.redstone_utils.server.gamerule.RedstoneUtilsGameRules;
import io.github.redstone787.redstone_utils.server.history.ChangeHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneUtils implements ModInitializer {
    public static final String MOD_ID = "redstone_utils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RedstoneUtilsGameRules.init();
        RedstoneUtilsServerConfig.load();
        RedstoneUtilsNetworking.init();
        ChangeHistory.init();
        ServerAutoWire.init();
        RedstoneUtilsCommands.init();
        RedstoneUtilsServerNetworking.init();
    }
}
