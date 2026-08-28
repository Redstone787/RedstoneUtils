/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.bud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudSwitchOverlayTest {

    @Test
    void detectsRetractedQuasiPoweredComponentEvenWhenMechanicallyBlocked() {
        assertTrue(BudStateDetector.isArmedQuasiState(false, true, false));
    }

    @Test
    void detectsActiveComponentWaitingForQuasiPowerRemovalUpdate() {
        assertTrue(BudStateDetector.isArmedQuasiState(false, false, true));
    }

    @Test
    void ignoresSynchronizedAndDirectlyPoweredComponents() {
        assertFalse(BudStateDetector.isArmedQuasiState(false, false, false));
        assertFalse(BudStateDetector.isArmedQuasiState(false, true, true));
        assertFalse(BudStateDetector.isArmedQuasiState(true, true, false));
    }
}
