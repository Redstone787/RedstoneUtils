package org.main.redstoneutils.client.autowire;

import net.minecraft.resources.Identifier;
import org.main.redstoneutils.RedstoneUtils;

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

    public String getDisplayName() {
        return displayName;
    }

    public Identifier getTextureIdentifier() {
        if (texture == null || texture.isBlank()) return null;
        if (texture.contains(":")) return Identifier.parse(texture);

        return RedstoneUtils.id(texture);
    }
}
