/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.signal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComparatorSignalTest {

    @Test
    void mapsBoundaryStrengthsToContainerAmounts() {
        assertEquals(0, ComparatorSignal.amountForSignal(0, 27, 64));
        assertEquals(1, ComparatorSignal.amountForSignal(1, 27, 64));
        assertEquals(1728, ComparatorSignal.amountForSignal(15, 27, 64));
    }

    @Test
    void saturatesVeryLargeCapacities() {
        assertEquals(Integer.MAX_VALUE, ComparatorSignal.amountForSignal(15, Integer.MAX_VALUE, 64));
    }
}
