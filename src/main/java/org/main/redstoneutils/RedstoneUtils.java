package org.main.redstoneutils;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.main.redstoneutils.network.RedstoneUtilsNetworking;
import org.main.redstoneutils.server.RedstoneUtilsCommands;
import org.main.redstoneutils.server.RedstoneUtilsServerNetworking;
import org.main.redstoneutils.server.autowire.ServerAutoWire;

public class RedstoneUtils implements ModInitializer {
    public static final String MOD_ID = "redstoneutils";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RedstoneUtilsNetworking.init();
        ServerAutoWire.init();
        RedstoneUtilsCommands.init();
        RedstoneUtilsServerNetworking.init();
    }
}
