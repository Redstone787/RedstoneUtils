/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstonelabworks.client.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.Identifier;
import io.github.redstone787.redstonelabworks.RedstoneLabworks;
import io.github.redstone787.redstonelabworks.client.config.RedstoneLabworksConfig;

import java.util.List;

public class RedstoneOverlay extends Overlay implements HudElement {

    private static final Identifier POPUP_LAYER = RedstoneLabworks.id("popup_messages");
    private static final RedstoneOverlay OVERLAY = new RedstoneOverlay();
    private static boolean visible = RedstoneLabworksConfig.isHudOverlayVisible();

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, POPUP_LAYER, OVERLAY);
    }

    public static void showPopup(String message) {
        PopupOverlay.show(message);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean visible) {
        RedstoneOverlay.visible = visible;
        RedstoneLabworksConfig.setHudOverlayVisible(visible);
        if (!visible) {
            SegmentWheelOverlay.setVisible(false);
        }
    }

    public static boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public static void setSegmentWheel(boolean visible, int segmentCount, List<Identifier> textures) {
        SegmentWheelOverlay.set(visible, segmentCount, textures, CircleSegment.NONE);
    }

    public static void setSegmentWheel(boolean visible, int segmentCount, List<Identifier> textures, CircleSegment highlightedSegment) {
        SegmentWheelOverlay.set(visible, segmentCount, textures, highlightedSegment);
    }

    public static void setSegmentWheelVisible(boolean visible) {
        SegmentWheelOverlay.setVisible(visible);
    }

    public static void setSegmentWheelCount(int segmentCount) {
        SegmentWheelOverlay.setCount(segmentCount);
    }

    public static void setSegmentWheelTextures(List<Identifier> textures) {
        SegmentWheelOverlay.setTextures(textures);
    }

    public static boolean isSegmentWheelVisible() {
        return SegmentWheelOverlay.isVisible();
    }

    public static CircleSegment getSelectedWheelSegment() {
        return SegmentWheelOverlay.getSelectedSegment();
    }

    public static CircleSegment consumeConfirmedWheelSegment() {
        return SegmentWheelOverlay.consumeConfirmedSegment();
    }

    public static CircleSegment finishSegmentWheelSelection() {
        return SegmentWheelOverlay.finishSelection();
    }

    public static Identifier redstoneTexture(String path) {
        return RedstoneLabworks.id(path);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        render(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        render(graphics);
    }

    private static void render(GuiGraphicsExtractor graphics) {
        if (!visible) return;

        PopupOverlay.render(graphics);
        SegmentWheelOverlay.render(graphics);
    }
}
