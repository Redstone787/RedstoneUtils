/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client;

import net.minecraft.network.chat.Component;
import io.github.redstone787.redstonelabworks.client.autowire.AutoWirePreviewOverlay;
import io.github.redstone787.redstonelabworks.client.bud.BudSwitchOverlay;
import io.github.redstone787.redstonelabworks.client.config.ConfigScreen;
import io.github.redstone787.redstonelabworks.client.macro.MacrosScreen;
import io.github.redstone787.redstonelabworks.client.sculk.SculkSensorOverlay;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneMessages;
import io.github.redstone787.redstonelabworks.client.ui.RedstoneOverlay;
import io.github.redstone787.redstonelabworks.client.ui.ToolboxScreen;

public final class RedstoneLabworksClientActions {

    public static final String CONFIG = "config";
    public static final String MACROS = "macros";
    public static final String WIRE_OVERLAY = "wire_overlay";
    public static final String SCULK_OVERLAY = "sculk_overlay";
    public static final String BUD_OVERLAY = "bud_overlay";
    public static final String ALL_OVERLAYS = "all_overlays";
    public static final String TOOLBOX = "toolbox";

    private RedstoneLabworksClientActions() {
    }

    public static void run(String action, int value) {
        switch (action) {
            case CONFIG -> openConfig();
            case MACROS -> openMacros();
            case WIRE_OVERLAY -> setWireOverlay(valueToBoolean(value, !AutoWirePreviewOverlay.isVisible()));
            case SCULK_OVERLAY -> setSculkOverlay(valueToBoolean(value, !SculkSensorOverlay.isVisible()));
            case BUD_OVERLAY -> setBudOverlay(valueToBoolean(value, !BudSwitchOverlay.isVisible()));
            case ALL_OVERLAYS -> setAllOverlays(valueToBoolean(value, !allOverlaysVisible()));
            case TOOLBOX -> ToolboxScreen.open();
            default -> {
            }
        }
    }

    public static void setWireOverlay(boolean visible) {
        AutoWirePreviewOverlay.setVisible(visible);
        overlayMessage("wire", visible);
    }

    public static boolean wireOverlayVisible() {
        return AutoWirePreviewOverlay.isVisible();
    }

    public static void setSculkOverlay(boolean visible) {
        SculkSensorOverlay.setVisible(visible);
        overlayMessage("sculk", visible);
    }

    public static boolean sculkOverlayVisible() {
        return SculkSensorOverlay.isVisible();
    }

    public static void setBudOverlay(boolean visible) {
        BudSwitchOverlay.setVisible(visible);
        overlayMessage("bud", visible);
    }

    public static boolean budOverlayVisible() {
        return BudSwitchOverlay.isVisible();
    }

    public static void setAllOverlays(boolean visible) {
        RedstoneOverlay.setVisible(visible);
        AutoWirePreviewOverlay.setVisible(visible);
        BudSwitchOverlay.setVisible(visible);
        SculkSensorOverlay.setVisible(visible);
        overlayMessage("all", visible);
    }

    public static boolean allOverlaysVisible() {
        return AutoWirePreviewOverlay.isVisible()
                && BudSwitchOverlay.isVisible()
                && RedstoneOverlay.isVisible()
                && SculkSensorOverlay.isVisible();
    }

    public static void openConfig() {
        ConfigScreen.open();
    }

    public static void openMacros() {
        MacrosScreen.open();
    }

    private static boolean valueToBoolean(int value, boolean toggleValue) {
        if (value == 0) return false;
        if (value == 1) return true;
        return toggleValue;
    }

    private static void overlayMessage(String overlay, boolean visible) {
        RedstoneMessages.popup(Component.translatable(
                "message.redstonelabworks.overlay." + overlay,
                Component.translatable(visible ? "state.redstonelabworks.on" : "state.redstonelabworks.off")
        ));
    }
}
