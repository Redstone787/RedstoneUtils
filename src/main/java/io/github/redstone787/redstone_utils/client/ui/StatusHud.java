/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.redstone787.redstone_utils.client.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import io.github.redstone787.redstone_utils.RedstoneUtils;
import io.github.redstone787.redstone_utils.client.RedstoneUtilsClientActions;
import io.github.redstone787.redstone_utils.client.RedstoneUtilsClientNetworking;
import io.github.redstone787.redstone_utils.client.autowire.AutoWireHandler;
import io.github.redstone787.redstone_utils.client.config.RedstoneUtilsConfig;
import io.github.redstone787.redstone_utils.client.overlay.OverlayFreeze;

import java.util.ArrayList;
import java.util.List;

public final class StatusHud implements HudElement {

    private static final Identifier ID = RedstoneUtils.id("status_hud");
    private static final StatusHud INSTANCE = new StatusHud();

    private StatusHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, ID, INSTANCE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RedstoneUtilsConfig.isStatusHudVisible()
                || minecraft.player == null
                || minecraft.getDebugOverlay().showDebugScreen()) return;
        Font font = minecraft.font;
        List<String> lines = lines();
        int width = lines.stream().mapToInt(font::width).max().orElse(0) + 12;
        int height = lines.size() * (font.lineHeight + 2) + 8;
        int margin = 6;
        boolean right = RedstoneUtilsConfig.getStatusHudAnchor() == RedstoneUtilsConfig.HudAnchor.TOP_RIGHT
                || RedstoneUtilsConfig.getStatusHudAnchor() == RedstoneUtilsConfig.HudAnchor.BOTTOM_RIGHT;
        boolean bottom = RedstoneUtilsConfig.getStatusHudAnchor() == RedstoneUtilsConfig.HudAnchor.BOTTOM_LEFT
                || RedstoneUtilsConfig.getStatusHudAnchor() == RedstoneUtilsConfig.HudAnchor.BOTTOM_RIGHT;
        int x = right ? graphics.guiWidth() - width - margin : margin;
        int y = bottom ? graphics.guiHeight() - height - margin : margin;
        graphics.fill(x, y, x + width, y + height, 0xC5222428);
        graphics.outline(x, y, width, height, 0xE06B7078);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), x + 6, y + 5 + index * (font.lineHeight + 2), 0xFFFFFFFF, false);
        }
    }

    private static List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.redstone_utils.autowire", AutoWireHandler.getActiveWireType().getDisplayName()).getString());
        List<String> overlays = new ArrayList<>();
        if (RedstoneUtilsClientActions.wireOverlayVisible()) overlays.add(Component.translatable("overlay.redstone_utils.wire").getString());
        if (RedstoneUtilsClientActions.budOverlayVisible()) overlays.add(Component.translatable("overlay.redstone_utils.bud").getString());
        if (RedstoneUtilsClientActions.sculkOverlayVisible()) overlays.add(Component.translatable("overlay.redstone_utils.sculk").getString());
        lines.add(Component.translatable("hud.redstone_utils.overlays", overlays.isEmpty() ? "-" : String.join(", ", overlays)).getString());
        lines.add(Component.translatable("hud.redstone_utils.backend", Component.translatable(
                RedstoneUtilsClientNetworking.hasServerBackend() ? "state.redstone_utils.available" : "state.redstone_utils.client_only"
        )).getString());
        if (OverlayFreeze.anyFrozen()) lines.add(Component.translatable("hud.redstone_utils.frozen").getString());
        return lines;
    }
}
