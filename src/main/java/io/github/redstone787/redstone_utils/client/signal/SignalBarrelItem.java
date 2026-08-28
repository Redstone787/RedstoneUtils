/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.signal;

final class SignalBarrelItem {

    private SignalBarrelItem() {
    }

    static String create(int strength) {
        return SignalBlockItem.createBarrel(strength);
    }
}
