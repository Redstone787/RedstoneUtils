/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.macro;

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
        if (!isBound(keyCode)) return Component.translatable("macro.redstone_utils.unbound").getString();

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        Component displayName = key.getDisplayName();
        String label = displayName == null ? "" : displayName.getString();
        if (!label.isBlank()) return label;

        String name = key.getName();
        return name == null || name.isBlank()
                ? Component.translatable("macro.redstone_utils.key", keyCode).getString()
                : name;
    }

    public static String displayName(int code, boolean mouseButton, int modifiers) {
        StringBuilder label = new StringBuilder();
        if ((modifiers & MOD_CONTROL) != 0) label.append(Component.translatable("modifier.redstone_utils.control").getString()).append('+');
        if ((modifiers & MOD_SHIFT) != 0) label.append(Component.translatable("modifier.redstone_utils.shift").getString()).append('+');
        if ((modifiers & MOD_ALT) != 0) label.append(Component.translatable("modifier.redstone_utils.alt").getString()).append('+');
        if ((modifiers & MOD_SUPER) != 0) label.append(Component.translatable("modifier.redstone_utils.super").getString()).append('+');
        if (mouseButton) label.append(Component.translatable("macro.redstone_utils.mouse", code + 1).getString());
        else label.append(displayName(code));
        return label.toString();
    }
}
