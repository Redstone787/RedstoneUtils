package org.main.redstoneutils.client.macro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public final class MacroKeys {

    public static final int MOD_SHIFT = 1;
    public static final int MOD_CONTROL = 2;
    public static final int MOD_ALT = 4;
    public static final int MOD_SUPER = 8;
    public static final int ALL_MODIFIERS = MOD_SHIFT | MOD_CONTROL | MOD_ALT | MOD_SUPER;

    private MacroKeys() {
    }

    public static boolean isBound(int keyCode) {
        return keyCode >= 0;
    }

    public static String displayName(int keyCode) {
        if (!isBound(keyCode)) return Component.translatable("macro.redstoneutils.unbound").getString();

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        Component displayName = key.getDisplayName();
        String label = displayName == null ? "" : displayName.getString();
        if (!label.isBlank()) return label;

        String name = key.getName();
        return name == null || name.isBlank()
                ? Component.translatable("macro.redstoneutils.key", keyCode).getString()
                : name;
    }

    public static String displayName(int code, boolean mouseButton, int modifiers) {
        StringBuilder label = new StringBuilder();
        if ((modifiers & MOD_CONTROL) != 0) label.append(Component.translatable("modifier.redstoneutils.control").getString()).append('+');
        if ((modifiers & MOD_SHIFT) != 0) label.append(Component.translatable("modifier.redstoneutils.shift").getString()).append('+');
        if ((modifiers & MOD_ALT) != 0) label.append(Component.translatable("modifier.redstoneutils.alt").getString()).append('+');
        if ((modifiers & MOD_SUPER) != 0) label.append(Component.translatable("modifier.redstoneutils.super").getString()).append('+');
        if (mouseButton) label.append(Component.translatable("macro.redstoneutils.mouse", code + 1).getString());
        else label.append(displayName(code));
        return label.toString();
    }
}
