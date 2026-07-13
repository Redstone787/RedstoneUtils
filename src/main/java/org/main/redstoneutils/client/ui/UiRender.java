package org.main.redstoneutils.client.ui;

import net.minecraft.util.Mth;

final class UiRender {

    private UiRender() {
    }

    static int scaleAlpha(int color, float opacity) {
        int alpha = color >>> 24;
        int scaledAlpha = Mth.clamp(Math.round(alpha * opacity), 0, 255);

        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }
}
