/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.autowire;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import io.github.redstone787.redstone_utils.RedstoneUtils;

import java.util.Locale;
import java.util.Optional;

public enum WireType {
    NONE("None", 0, "textures/gui/wire/none.png"),
    NORMAL("Normal", 1, "textures/gui/wire/normal.png"),
    AUTO("Auto", 2, "textures/gui/wire/auto.png"),
    FAST_AUTO("Fast Auto", 3, "textures/gui/wire/fast_auto.png"),
    ONLY_REPEATERS("Only Repeaters", 4, "textures/gui/wire/only_repeaters.png"),
    ONLY_COMPARATORS("Only Comparators", 5, "textures/gui/wire/only_comparators.png"),
    FAST_COMPARATORS("Fast Comparators", 6, "textures/gui/wire/fast_comparators.png");

    private final String displayName;
    private final int index;
    private final String texture;

    WireType(String displayName, int index, String texture) {
        this.displayName = displayName;
        this.index = index;
        this.texture = texture;
    }

    public int getIndex() {
        return index;
    }

    public static Optional<WireType> find(String name) {
        if (name == null) return Optional.empty();
        String normalized = name.strip().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String getDisplayName() {
        return Component.translatable("wire_type.redstone_utils." + name().toLowerCase(java.util.Locale.ROOT)).getString();
    }

    public Identifier getTextureIdentifier() {
        if (texture == null || texture.isBlank()) return null;
        if (texture.contains(":")) return Identifier.parse(texture);

        return RedstoneUtils.id(texture);
    }
}
