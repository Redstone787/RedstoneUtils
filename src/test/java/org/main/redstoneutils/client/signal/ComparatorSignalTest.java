package org.main.redstoneutils.client.signal;

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
