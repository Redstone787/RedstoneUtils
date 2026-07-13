package org.main.redstoneutils.client.signal;

final class SignalBarrelItem {

    private SignalBarrelItem() {
    }

    static String create(int strength) {
        return SignalBlockItem.createBarrel(strength);
    }
}
