package org.main.redstoneutils;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.main.redstoneutils.network.RedstoneUtilsNetworking;
import org.main.redstoneutils.server.RedstoneUtilsCommands;
import org.main.redstoneutils.server.RedstoneUtilsServerNetworking;
import org.main.redstoneutils.server.autowire.ServerAutoWire;
import org.main.redstoneutils.server.config.RedstoneUtilsServerConfig;
import org.main.redstoneutils.server.gamerule.RedstoneUtilsGameRules;
import org.main.redstoneutils.server.history.ChangeHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneUtils implements ModInitializer {
    public static final String MOD_ID = "redstoneutils";
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
