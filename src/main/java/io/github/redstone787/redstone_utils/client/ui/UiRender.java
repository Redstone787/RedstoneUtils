/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.ui;

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
