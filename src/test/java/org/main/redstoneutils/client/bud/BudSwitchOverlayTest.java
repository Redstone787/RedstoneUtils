package org.main.redstoneutils.client.bud;

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
