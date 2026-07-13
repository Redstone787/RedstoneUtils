package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public final class MacroKeys {

    private MacroKeys() {
    }

    public static boolean isBound(int keyCode) {
        return keyCode >= 0;
    }

    public static String displayName(int keyCode) {
        if (!isBound(keyCode)) return "Unbound";

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        Component displayName = key.getDisplayName();
        String label = displayName == null ? "" : displayName.getString();
        if (!label.isBlank()) return label;

        String name = key.getName();
        return name == null || name.isBlank() ? "Key " + keyCode : name;
    }
}
