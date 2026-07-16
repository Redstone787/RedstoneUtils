package org.main.redstoneutils.client;

import org.main.redstoneutils.client.autowire.AutoWirePreviewOverlay;
import org.main.redstoneutils.client.config.ConfigScreen;
import org.main.redstoneutils.client.macro.MacrosScreen;
import org.main.redstoneutils.client.sculk.SculkSensorOverlay;
import org.main.redstoneutils.client.ui.RedstoneMessages;
import org.main.redstoneutils.client.ui.RedstoneOverlay;

public final class RedstoneUtilsClientActions {

    public static final String CONFIG = "config";
    public static final String MACROS = "macros";
    public static final String WIRE_OVERLAY = "wire_overlay";
    public static final String SCULK_OVERLAY = "sculk_overlay";
    public static final String ALL_OVERLAYS = "all_overlays";

    private RedstoneUtilsClientActions() {
    }

    public static void run(String action, int value) {
        switch (action) {
            case CONFIG -> openConfig();
            case MACROS -> openMacros();
            case WIRE_OVERLAY -> setWireOverlay(valueToBoolean(value, !AutoWirePreviewOverlay.isVisible()));
            case SCULK_OVERLAY -> setSculkOverlay(valueToBoolean(value, !SculkSensorOverlay.isVisible()));
            case ALL_OVERLAYS -> setAllOverlays(valueToBoolean(value, !allOverlaysVisible()));
            default -> {
            }
        }
    }

    public static void setWireOverlay(boolean visible) {
        AutoWirePreviewOverlay.setVisible(visible);
        RedstoneMessages.popup("Wire overlay: " + stateName(visible));
    }

    public static boolean wireOverlayVisible() {
        return AutoWirePreviewOverlay.isVisible();
    }

    public static void setSculkOverlay(boolean visible) {
        SculkSensorOverlay.setVisible(visible);
        RedstoneMessages.popup("Sculk overlay: " + stateName(visible));
    }

    public static boolean sculkOverlayVisible() {
        return SculkSensorOverlay.isVisible();
    }

    public static void setAllOverlays(boolean visible) {
        RedstoneOverlay.setVisible(visible);
        AutoWirePreviewOverlay.setVisible(visible);
        SculkSensorOverlay.setVisible(visible);
        RedstoneMessages.popup("All overlays: " + stateName(visible));
    }

    public static boolean allOverlaysVisible() {
        return AutoWirePreviewOverlay.isVisible() && RedstoneOverlay.isVisible() && SculkSensorOverlay.isVisible();
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

    private static String stateName(boolean visible) {
        return visible ? "on" : "off";
    }
}
