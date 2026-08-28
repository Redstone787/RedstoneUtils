/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.bud;

final class BudStateDetector {

    private BudStateDetector() {
    }

    static boolean isArmedQuasiState(boolean directlyPowered, boolean quasiPowered, boolean active) {
        return !directlyPowered && quasiPowered != active;
    }
}
