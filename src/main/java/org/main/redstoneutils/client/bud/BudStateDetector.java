package org.main.redstoneutils.client.bud;

final class BudStateDetector {

    private BudStateDetector() {
    }

    static boolean isArmedQuasiState(boolean directlyPowered, boolean quasiPowered, boolean active) {
        return !directlyPowered && quasiPowered != active;
    }
}
