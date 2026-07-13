package org.main.redstoneutils;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class RedstoneUtils implements ModInitializer {
    public static final String MOD_ID = "redstoneutils";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
    }
}
